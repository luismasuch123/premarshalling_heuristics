import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PreMarshallingJovanovic {

    static boolean consider_time;

    static String next_selection;
    static String stack_selection;
    static String stack_filling;

    static int stacks;
    static int tiers;
    static int containers;

    static int [][][] initial_bay;
    static int [][][] current_bay;
    static int [][][] copy;

    static int [][] bay_info = new int [5][2]; //1st/2nd and 3rd highest top stack due date value of not well located stacks (prio and stack) and -1 at position 4 and/or 5 if there are empyty stacks
    static int [][] s_info; //highest tier, ordered
    static int [][] c_info; //stack, tier, prio, sorted

    static int [][]  g_c_s; //number of containers above c in stack s
    static int [][] f_c_s; //containers above well located block c_a with larger or same due date value than c in destination stack s
    static int [][]nw_c_s; //well located containers that need to be relocated when c is moved to destination stack s
    static int [][] f_c_s_ext; //berücksichtigt zusätzlich zu f_c_s, dass Umlagerungen von bereits richtig platzierten Blöcken vermieden werden sollten
    static int [][] w_c_s; //Summe der Blöcke, die bei einer Umlagerung von c aus stack s in destination stack ss, aus s und ss heraus umgelagert werden müssen
    static int [] d_c; //d_c is the index of the stack that the lowest value of w_c_s for a block c and if tied the minimum distance to c

    static boolean same_stack;
    static boolean same_stack_over;
    static boolean same_stack_under;
    static boolean next_to_stopover_stack_prevent_deadlock;
    static int stopover_stack_prevent_deadlock;
    static boolean deadlock;
    static int deadlock_count;
    static int [] deadlock_next;
    static int [] order_relocations;
    static int [] selected_stacks;
    static int [] same_stack_below;
    static int next;

    static int step = 1;
    static int relocations = 0;
    static int [] distance_relocations = new int[3];
    static double time_relocations [] = new double [2];


    public static void main (String [] args) throws FileNotFoundException {
        String initial_bay_path = "/Users/luismasuchibanez/IdeaProjects/premarshalling_heuristics/data/Test/emm_s10_t4_p1_c0_16.bay";
        consider_time = true;
        next_selection = "function h_c"; //"highest due date value"
        //TODO: certain improvements can be achieved by adding some fine tuning (stack_selection)
        stack_selection = "The Lowest Position";//"Lowest Priority First", "MinMax"
        stack_filling = " ";
        try {
            get_initial_bay(initial_bay_path);
            //check if blocks are well located or not (1:well located, 0: not well located)
            compute_if_well_located(initial_bay);
            System.out.println("Initial bay: " + Arrays.deepToString(initial_bay));

            int [][][] final_bay = premarshall(initial_bay);
            time_relocations[0] = ((double) distance_relocations[0] * 2 * 15) /3600;
            time_relocations[1] = ((double) distance_relocations[1] * 2 * 2.4) /3600;

            System.out.println("Final bay: " + Arrays.deepToString(final_bay));
            System.out.println("Relocations: " + relocations);
            System.out.println("Distance_relocations in blocks: " + Arrays.toString(distance_relocations));
            System.out.println("Time_relocations in: " + Arrays.toString(time_relocations));
            if (deadlock_count > 0) {
                System.out.println("Deadlocks: " + deadlock_count);
            }
        } catch (FileNotFoundException e) {
            System.out.println(("File not found!"));
        }
        
    }

    private static int[][][] premarshall(int[][][] initial_bay) {
        current_bay = new int[stacks][tiers][3];
        current_bay = copy_bay(initial_bay, current_bay);

        s_info = new int [stacks][2];
        c_info = new int[containers][4];

        copy = new int[stacks][tiers][3]; //Kopie, damit current_bay nicht verändert wird, falls Schritt rückgängig gemacht werden muss nach look-ahead

        boolean sorted = false; //TODO: Möglichkeit, dass bereits geordnet ist einbauen?
        deadlock = false;
        deadlock_next = new int[containers];
        while(!sorted) {
            System.out.println("Step " + step);
            System.out.println("Current bay: " + Arrays.deepToString(current_bay));
            step++;

            copy = copy_bay(current_bay, copy);

            g_c_s = new int[containers][stacks];
            compute__c_info__s_info__g_c_s();

            //calculating bay_info
            compute_bay_info();

            System.out.println("c_info: " + Arrays.deepToString(c_info));
            System.out.println("s_info: " + Arrays.deepToString(s_info));
            //System.out.println("bay_info: " + Arrays.deepToString(bay_info));
            //System.out.println("g_c_s: " + Arrays.deepToString(g_c_s));

            //TODO: wirklich larger or same due date value?
            f_c_s = new int[containers][stacks];
            nw_c_s = new int[containers][stacks];
            compute__f_c_s__nw_c_s();
            //System.out.println("f_c_s: " + Arrays.deepToString(f_c_s));
            //System.out.println("nw_c_s: " + Arrays.deepToString(nw_c_s));

            f_c_s_ext = new int[containers][stacks];
            compute__f_c_s_ext();
            //System.out.println("f_c_s_ext: " + Arrays.deepToString(f_c_s_ext));

            w_c_s = new int[containers][stacks];
            compute__w_c_s();
            //System.out.println("w_c_s: " + Arrays.deepToString(w_c_s));

            d_c = new int [containers];
            compute_d_c();
            System.out.println("d_c: " + Arrays.toString(d_c));

            get_next();
            System.out.println("next: " + next + " from stack " + c_info[next][0] + " to stack " + d_c[next]);

            same_stack = false;
            same_stack_over = false;
            same_stack_under = false;
            next_to_stopover_stack_prevent_deadlock = false;

            compute__order_relocations__same_stack_below(next);
            System.out.println("order_relocations: " + Arrays.toString(order_relocations));
            if (same_stack_under) {
                System.out.println("same_stack_below: " + Arrays.toString(same_stack_below));
            }

            //die Blöcke umlagern, die next blockieren (egal welcher stack) oder next's Platz im destination stack blockieren (wenn stack gleich destination stack)
            relocate_blocking_blocks(); //TODO: relocations dürfen nicht vor next durchgeführt werden, falls sie in stack s von next hinein umgelagert werden, dann

            //next umlagern
            //TODO: wann kann/muss deadlock gesetzt und wieder entfernt werden?
            if (!deadlock) {
                deadlock = false;
                deadlock_next = new int[containers];
                relocate_next(); //TODO: immer prüfen, ob über next der Weg frei ist für Umlagerung -> generell in relocate prüfen
            } else {
                System.out.println("DEADLOCK!");
                deadlock_next[next] = next+1;
                System.out.println("deadlock_next: " + Arrays.toString(deadlock_next));
                deadlock_count++;
                copy = copy_bay(current_bay, copy);
            }


            //TODO: avoiding deadlocks -> wenn deadlock copy auf current_bay zurücksetzen und boolean-flags für verbotene moves bzw. verbotenen next-Block nutzen
            //TODO: stopover stack wenn s = ds, derjenige mit einem freien Platz, wenn freie_Plätze(abgesehen von Plätzen in s) - f_c_s = 1
            current_bay = copy_bay(copy, current_bay);

            boolean stacks_sorted = true;
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][1] == 0) {
                    stacks_sorted = false;
                }
            }
            if (stacks_sorted) {
                sorted = true;
            }
            //TODO: Distanzen bei Leerfahrten berücksichtigen
        }
        //TODO: Corrections
        return current_bay;
    }

    private static void compute__w_c_s() {
        for (int c = 0; c < containers; c++) {
            for (int s = 0; s < stacks; s++) {
                if (c_info[c][0] != s) {
                    w_c_s[c][s] = f_c_s_ext[c][s] + g_c_s[c][c_info[c][0]] + 1;
                } else {
                    w_c_s[c][s] = f_c_s_ext[c][s] + 1;
                }
            }
        }
    }

    private static void compute__f_c_s_ext() {
        for (int c = 0; c < containers; c++) {
            for (int s = 0; s < stacks; s++) {
                f_c_s_ext[c][s] = f_c_s[c][s] + nw_c_s[c][s];
            }
        }
    }

    private static void get_next() {
        boolean next_option_found = false;
        TreeSet<Integer> next_options_set = new TreeSet<>();
        for (int c = 0; c < containers; c++) {
            if (!deadlock) {
                if (c_info[c][3] == 0) {
                    next_option_found = true;
                    next_options_set.add(c);
                    deadlock = false;
                }
            } else {
                if (c_info[c][3] == 0 && deadlock_next[c] != c+1) {
                    next_option_found = true;
                    next_options_set.add(c);
                }
            }
        }
        if (next_option_found) {
            Object[] next_options = next_options_set.toArray();
            if (next_selection == "function h_c") {
                next = get_next_funtion_h_c(next, d_c, w_c_s, next_options, next_option_found);
            } else if (next_selection == "highest due date value") {
                next = get_next_highest_due_date_value(next, d_c, next_options, next_option_found);
            }
        } else {
            System.out.println("DEADLOCK! No next_option found.");
            System.exit(0);
        }
    }

    private static void relocate_next() {
        if (next_to_stopover_stack_prevent_deadlock) {
            int block = next;
            int prio = c_info[next][2];
            int next_stack = stopover_stack_prevent_deadlock;

            relocate(block, prio, next_stack);
            next_to_stopover_stack_prevent_deadlock = false;
        } else if (!same_stack_under) {
            int block = next;
            int prio = c_info[next][2];
            int next_stack = d_c[next];

            relocate(block, prio, next_stack);
        } else {
            //wenn s gleich ds und noch ein Block unter c umgelagert werden muss bevor c an Zielstelle gelangt, muss c erst in einen anderen Block
            //erst stack für below aussuchen, da c nur zu nächstem Stack sollte
            int [][] s_info_help = new int [stacks][2];
            for (int s = 0; s < stacks; s++) {
                s_info_help[s][0] = s_info[s][0];
                s_info_help[s][1] = s_info[s][1];
            }
            int selected_stack [] = new int [same_stack_below.length];
            for (int c = 0; c < same_stack_below.length; c++) {
                TreeSet<Integer> stack_options = new TreeSet<>();
                if (stack_selection == "The Lowest Position") {
                    int minimum_stack_height = tiers;
                    for (int s = 0; s < stacks; s++) {
                        if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                            if (! ((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
                                if (s_info_help[s][0] < minimum_stack_height) {
                                    minimum_stack_height = s_info_help[s][0];
                                    stack_options.clear();
                                    stack_options.add(s);
                                } else if (s_info_help[s][0] == minimum_stack_height) {
                                    stack_options.add(s);
                                }
                            }
                        }
                    }
                    if (stack_options.size() == 0) {
                        deadlock = true;
                    } else if (stack_options.size() == 1 || !consider_time) {
                        selected_stack[c] = stack_options.first();
                    } else if (consider_time) {
                        double time_to_destination_stack = 1000000;
                        for (int stack : stack_options) {
                            double time_to_destination_stack_option = (double) Math.abs(c_info[same_stack_below[c]][0] - stack) * 2.4 + (double) ((tiers - c_info[same_stack_below[c]][1]) + (tiers - (s_info_help[stack][0]))) * 15.0;
                            if (time_to_destination_stack_option < time_to_destination_stack) {
                                selected_stack[c] = stack;
                            }
                        }
                    }
                } else if (stack_selection == "Lowest Priority Index") {

                } else if (stack_selection == "MinMax") {

                }
                //da Umlagerung noch nicht direkt durchgeführt wird, muss sichergestellt werden, dass nicht mehr Blöcke als möglich in einen Stack hinein umgelagert werden
                if (stack_options.size() != 0) {
                    s_info_help[selected_stack[c]][0] += 1;
                }
            }
            System.out.println("selected_stack: " + Arrays.toString(selected_stack));

            if (!deadlock) {
                //next in nächsten Stack s umgelagern, der keinem der Stacks aus selected_stack[] entspricht (am besten der nächste Platz)
                double time_to_stopover_stack = 100000;
                int stopover_stack = 0;
                for (int s = 0; s < stacks; s++) {
                    //prüft, ob s einem der selected_stacks für same_stack_below-Blöcke entspricht
                    //TODO: einfacher mit Methode (contains) prüfen ob in selected_stacks enthalten
                    boolean selected_stacks_contains_s = false;
                    for (int ss = 0; ss < selected_stack.length; ss++) {
                        if (s == selected_stack[ss]) {
                            selected_stacks_contains_s = true;
                            break;
                        }
                    }
                    //stopover_stack darf weder einem der selected_stacks für same_stack_below-Blöcke, noch dem destination stack entsprechen
                    //außerdem darf stopover_stack nicht voll sein
                    if (!selected_stacks_contains_s && s != d_c[next] && s_info[s][0] < tiers - 1) {
                        if (consider_time) {
                            double time_to_stopover_stack_option = (double) Math.abs(c_info[next][0] - s) * 2.4 + (double) ((tiers - c_info[next][1]) + (tiers - (s_info[s][0]))) * 15.0;
                            if (time_to_stopover_stack_option < time_to_stopover_stack) {
                                time_to_stopover_stack = time_to_stopover_stack_option;
                                stopover_stack = s;
                            }
                        } else {
                            stopover_stack = s;
                            s = stacks;
                            break; //TODO: klappt das? oder "break outer;"?
                        }
                    }
                }
                //next in stopover_stack umlagern
                int block = next;
                int prio = c_info[block][2];
                int next_stack = stopover_stack;

                relocate(block, prio, next_stack);

                //jeweils Block c aus same_stack_below[c] in selected_stack[c] umlagern
                for (int c = 0; c < same_stack_below.length; c++) {
                    block = same_stack_below[c];
                    prio = c_info[block][2];
                    next_stack = selected_stack[c];

                    relocate(block, prio, next_stack);
                }

                //next von stopover_stack in ds umlagern
                block = next;
                prio = c_info[block][2];
                next_stack = d_c[next];

                relocate(block, prio, next_stack);
            }
        }
    }

    private static void relocate_blocking_blocks() {
        get_selected_stacks();

        if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
            //relocation durchführen in order_relocation und Updates
            for (int c = 0; c < order_relocations.length; c++) {
                int block = order_relocations[c];
                int prio = c_info[block][2];
                int next_stack = selected_stacks[c];

                relocate(block, prio, next_stack);
            }
        } else if (next_to_stopover_stack_prevent_deadlock && !deadlock) {
            relocate_next();
            get_selected_stacks();
            if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
                //relocation durchführen in order_relocation und Updates
                for (int c = 0; c < order_relocations.length; c++) {
                    int block = order_relocations[c];
                    int prio = c_info[block][2];
                    int next_stack = selected_stacks[c];

                    relocate(block, prio, next_stack);
                }
            }
        }
    }

    private static void get_selected_stacks() {
        int [][] s_info_help = new int [stacks][2];
        for (int s = 0; s < stacks; s++) {
            s_info_help[s][0] = s_info[s][0];
            s_info_help[s][1] = s_info[s][1];
        }
        selected_stacks = new int [order_relocations.length];
        stopover_stack_prevent_deadlock = 0;
        for (int c = 0; c < order_relocations.length; c++) {
            TreeSet<Integer> stack_options = new TreeSet<>();
            if (stack_selection == "The Lowest Position") {
                int minimum_stack_height = tiers;
                for (int s = 0; s < stacks; s++) {
                    //prüfen, ob s nicht (gleich ds und Blöcke über next liegen)
                    if (!(same_stack_over && s == c_info[order_relocations[c]][0])) {
                        //prüfen, ob s nicht gleich aktueller stack, s nicht gleich ds von next und ob stack schon voll
                        if (s != c_info[next][0] && s != c_info[order_relocations[c]][0] && s != d_c[next] && (s_info_help[s][0] + 1) < tiers) {
                            if (s_info_help[s][0] < minimum_stack_height) {
                                minimum_stack_height = s_info_help[s][0];
                                stack_options.clear();
                                stack_options.add(s);
                            } else if (s_info_help[s][0] == minimum_stack_height) {
                                stack_options.add(s);
                            }
                        }
                        //prüfen, ob prio von c zwischen next und Block unter destination Platz und liegt genügend Platz in ds
                        /*
                        else if ((c_info[order_relocations[c]][2] >= c_info[next][2]) && (c_info[order_relocations[c]][2] <= copy[s][s_info[s][0]][1]) && ((s_info[s][0] + 2) < tiers)) {
                            if (s_info[s][0] < minimum_stack_height) {
                                minimum_stack_height = s_info[s][0];
                                stack_options.clear();
                                stack_options.add(s);
                            } else if (s_info[s][0] == minimum_stack_height) {
                                stack_options.add(s);
                            }
                        }
                        */ //TODO: prüfen, ob nicht doch umsetzbar (im Original nicht berücksichtigt), zu berücksichtigen ist, ob umzulagernder Block in s oder ds
                        //könnte wie bei stopover_stack zwischengelagert werden, wenn in ds noch nicht alle blockierenden Blöcke frei sind bzw. muss sicherlich zwischengelagert werden,
                        //denn wenn es wird aus beiden Stacks immer niedrigste Prio (hohe Zahl) gewählt, um umgelagert zu werden
                    }
                }
                if (stack_options.size() == 0) {
                    //relocate next to stopover_stack first before continuing with relocations
                    if (c_info[next][1] != tiers-1) {
                        if (copy[c_info[next][0]][c_info[next][1] + 1][0] == 0) {
                            next_to_stopover_stack_prevent_deadlock = true;
                            boolean stopover_stack_found = false;
                            for (int s = 0; s < stacks; s++) {
                                if (s != d_c[next] && s != c_info[next][0] && s_info[s][0] != tiers-1) {
                                    stopover_stack_prevent_deadlock = s;
                                    stopover_stack_found = true;
                                    c = order_relocations.length;
                                    break;
                                    //TODO: hier könnte time berücksichtigt werden
                                }
                            }
                            if (!stopover_stack_found) {
                                deadlock = true;
                                c = order_relocations.length;
                            }
                        } else {
                            deadlock = true;
                            c = order_relocations.length;
                        }
                    } else {
                        next_to_stopover_stack_prevent_deadlock = true;
                        for (int s = 0; s < stacks; s++) {
                            if (s != d_c[next] && s != c_info[next][0]) {
                                stopover_stack_prevent_deadlock = s;
                                relocate_next();
                                break;
                                //TODO: hier könnte time berücksichtigt werden
                            }
                        }
                    }

                } else if (stack_options.size() == 1 || !consider_time) {
                    selected_stacks[c] = stack_options.first();
                    s_info_help[selected_stacks[c]][0] += 1;
                } else if (consider_time) {
                    double time_to_destination_stack = 1000000;
                    for (int stack : stack_options) {
                        double time_to_destination_stack_option = (double) Math.abs(c_info[order_relocations[c]][0] - stack) * 2.4 + (double) ((tiers - c_info[order_relocations[c]][1]) + (tiers - (s_info_help[stack][0]))) * 15.0;
                        if (time_to_destination_stack_option < time_to_destination_stack) {
                            selected_stacks[c] = stack;
                        }
                    }
                    s_info_help[selected_stacks[c]][0] += 1;
                }
            } else if (stack_selection == "Lowest Priority Index") {

            } else if (stack_selection == "MinMax") {

            }
        }
    }

    private static void compute__order_relocations__same_stack_below(int next) {
        //order in which blocks are relocated (indices)
        //current stack
        int stack_s = c_info[next][0];
        int current_height_s = s_info[stack_s][0];
        int blocking_blocks_s = g_c_s[next][stack_s];
        //destination stack
        int stack_ds = d_c[next];
        int current_height_ds = s_info[stack_ds][0];
        int blocking_blocks_ds = f_c_s[next][stack_ds];

        if (stack_s != stack_ds) {
            same_stack_below = new int[0];
            order_relocations = new int[blocking_blocks_s + blocking_blocks_ds];
            int i = 0;
            while (blocking_blocks_s + blocking_blocks_ds > 0) {
                if (blocking_blocks_s > 0 && blocking_blocks_ds > 0) {
                    if (copy[stack_s][current_height_s][1] > copy[stack_ds][current_height_ds][1]) {
                        order_relocations[i] = copy[stack_s][current_height_s][0] - 1;
                        current_height_s--;
                        blocking_blocks_s--;
                        i++;
                    } else if (copy[stack_s][current_height_s][1] < copy[stack_ds][current_height_ds][1]) {
                        order_relocations[i] = copy[stack_ds][current_height_ds][0] - 1;
                        current_height_ds--;
                        blocking_blocks_ds--;
                        i++;
                    } else {
                        //TODO: kann man hier eine Distanzberücksichtigung durchführen?
                        order_relocations[i] = copy[stack_s][current_height_s][0] - 1; //Übergangsweise
                        current_height_s--;
                        blocking_blocks_s--;
                        i++;
                    }
                } else if (blocking_blocks_s > 0) {
                    order_relocations[i] = copy[stack_s][current_height_s][0] - 1;
                    current_height_s--;
                    blocking_blocks_s--;
                    i++;
                } else if (blocking_blocks_ds > 0) {
                    order_relocations[i] = copy[stack_ds][current_height_ds][0] - 1;
                    current_height_ds--;
                    blocking_blocks_ds--;
                    i++;
                }
            }
        } else {
            same_stack = true;
            order_relocations = new int[blocking_blocks_s];
            if (blocking_blocks_ds - blocking_blocks_s - 1 < 0) {
                same_stack_below = new int[0];
            } else {
                //TODO: macht diese Berechnung Sinn? (müssen blocking_blocks_s überhaupt berücksichtigt werden?)
                same_stack_below = new int[blocking_blocks_ds - blocking_blocks_s - 1];
            }
            for (int i = 0; i < order_relocations.length; i++) {
                same_stack_over = true;
                order_relocations[i] = copy[stack_s][current_height_s][0] - 1;
                current_height_s--;
            }
            for (int i = 0; i < same_stack_below.length; i++) {
                same_stack_under = true;
                same_stack_below[i] = copy[stack_s][current_height_s-1][0] - 1;
                current_height_s--;
            }

        }
    }

    private static int get_next_highest_due_date_value(int next, int[] d_c, Object[] next_options, boolean next_option_found) {
        int highest_due_date = 0;
        double time_to_destination_stack = 1000000;
        if (next_option_found) {
            for (int cc = 0; cc < next_options.length; cc++) {
                int c = (int) next_options[cc];
                if (c_info[c][2] > highest_due_date) {
                    highest_due_date = c_info[c][2];
                    next = c;
                } else if (consider_time && c_info[c][2] == highest_due_date) {
                    int next_option = c;
                    double time_to_destination_stack_option = (double) Math.abs(c_info[next_option][0] - d_c[next_option]) * 2.4 + (double) ((tiers - c_info[next_option][1]) + (tiers - (s_info[d_c[next_option]][0] - f_c_s[next_option][d_c[next_option]]))) * 15.0;
                    if (time_to_destination_stack_option < time_to_destination_stack) {
                        highest_due_date = c_info[c][2];
                        next = c;
                    }
                }
            }
        } else {
            next = 0;
        }
        return next;
    }

    private static int get_next_funtion_h_c(int next, int[] d_c, int[][] w_c_s, Object[] next_options, boolean next_option_found) {
        //number of forced relocations: in this approximation, a forced relocation occurs only if a block c is being relocated and all of the top stack due date values are larger than p(c)
        //if all blocks in a stack are well located we shall consider the stack having due date value zero
        int[] fr_c_s = new int[containers];
        //TODO: werden im Paper nur forced relocations aus stack s oder auch aus destination stack ss betrachtet? -> hier werden beide betrachtet (Option mit oder ohne?)
        fr_c_s = compute_fr_c_s(fr_c_s, d_c, next_options);
        //System.out.println("fr_c_s: " + Arrays.toString(fr_c_s));

        int[] h_c = new int[containers];
        for (int cc = 0; cc < next_options.length; cc++) {
            int c = (int) next_options[cc];
            h_c[c] = w_c_s[c][d_c[c]] + fr_c_s[c] - c_info[c][2];
        }
        //System.out.println("h_c: " + Arrays.toString(h_c));

        //indice of next block to be relocated is calculated
        //TODO: can in most cases be calculated by evaluating h_c for a small number of blocks -> kaum Einsparung?!
        int min_value = 1000000; //TODO: jedes Mal Distanz vergleichen wenn gleicher Wert oder doch lieber Set und nur Distanz vergleichen wenn am Ende mehr als ein Eintrag?
        double time_to_destination_stack = 1000000;
        if (next_option_found) {
            for (int cc = 0; cc < next_options.length; cc++) {
                int c = (int) next_options[cc];
                if (h_c[c] < min_value) {
                    next = c;
                    min_value = h_c[c];
                    //TODO: distance-Berechnung überprüfen bzw. macht s_info Höhe=-1 Sinn?
                    time_to_destination_stack = (double) Math.abs(c_info[next][0] - d_c[next]) * 2.4 + (double) ((tiers - c_info[next][1]) + (tiers - (s_info[d_c[next]][0] - f_c_s[next][d_c[next]] + 1))) * 15.0;
                } else if (consider_time && h_c[c] == min_value) {
                    int next_option = c;
                    double time_to_destination_stack_option = (double) Math.abs(c_info[next_option][0] - d_c[next_option]) * 2.4 + (double) ((tiers - c_info[next_option][1]) + (tiers - (s_info[d_c[next_option]][0] - f_c_s[next_option][d_c[next_option]] + 1))) * 15.0;
                    if (time_to_destination_stack_option < time_to_destination_stack) {
                        min_value = h_c[c];
                        next = c;
                    }
                }
            }
        } else {
            next = 0;
        }
        return next;
    }

    private static int[] compute_fr_c_s(int[] fr_c_s, int[] d_c, Object[] next_options) {
        //wenn es zwei empty stacks gibt, kann es keine forced relocations geben
        if (! (bay_info[3][1] == -1 && bay_info[4][0] == -1)) {
            for (int cc = 0; cc < next_options.length; cc++) {
                int c = (int) next_options[cc];
                //wenn es mindestens einen empty stack gibt, dann kann es aus current stack heraus keine forced relocation geben
                if (bay_info[3][1] == 0) {
                    //in current stack
                    int stack = c_info[c][0];
                    int height = c_info[c][1];
                    int current_height = s_info[stack][0];
                    int blocking_blocks = g_c_s[c][stack];
                    while ((current_height - blocking_blocks) >= height) {
                        int cs_prio = copy[stack][current_height][1];
                        if (cs_prio < bay_info[0][0] && bay_info[0][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else if (cs_prio < bay_info[1][0] && bay_info[1][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else if (cs_prio < bay_info[2][0] && bay_info[2][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else {
                            current_height--;
                        }
                    }
                } else {
                    break;
                }
                //wenn es mehr als einen empty stack gibt oder der empty stack nicht dem destination stack entspricht, dann kann es aus destination stack heraus keine forced relocation geben
                if (!(bay_info[3][1] == d_c[c] && bay_info[4][1] == 0)) {
                    //in destination stack
                    int stack = d_c[c];
                    int height = s_info[stack][0] - f_c_s[c][stack];
                    int current_height = s_info[stack][0];
                    int blocking_blocks = f_c_s[c][stack];
                    while ((current_height - blocking_blocks) >= height && current_height != -1) {
                        int cs_prio = copy[stack][current_height][2];
                        if (cs_prio < bay_info[0][0] && bay_info[0][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else if (cs_prio < bay_info[1][0] && bay_info[1][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else if (cs_prio < bay_info[2][0] && bay_info[2][1] != stack) {
                            fr_c_s[c] += 1;
                            current_height--;
                        } else {
                            current_height--;
                        }
                    }
                } else {
                    break;
                }
            }
        }
        return fr_c_s;
    }

    private static void compute_d_c() {
        for(int c = 0; c < containers; c++) {
            //Liste mit den Indizes der niedrigsten w_c_s erstellen, um dann Distanzen zu vergleichen
            int lowest_w_c_s = 100000;
            Set<Integer> indices = new HashSet<Integer>();
            for(int s = 0; s < stacks; s++) {
                //Stack steht nur zur Auswahl, wenn nicht (voll und geordnet)
                if (! (s_info[s][0] == tiers-1 && copy[s][tiers-1][2] == 1)) {
                    if (w_c_s[c][s] < lowest_w_c_s) {
                        lowest_w_c_s = w_c_s[c][s];
                        indices.clear();
                        indices.add(s);
                    } else if (w_c_s[c][s] == lowest_w_c_s) {
                        indices.add(s);
                    }
                }

            }
            //indices mit gleichen w_c_s durchlaufen, um den Stack mit dem geringsten Abstand zu c zu finden
            if (consider_time) {
                double time_to_stack = 100000;
                for (int indice : indices) {
                    //Enfernungen mit passenden Faktoren belegen
                    double time_to_current_stack = (double) Math.abs(c_info[c][0] - indice) * 2.4 + (double) ((tiers - c_info[c][1]) + (tiers - (s_info[indice][0] - f_c_s[c][indice]))) * 15.0;
                    if (time_to_current_stack < time_to_stack) {
                        d_c[c] = indice;
                    }
                }
            } else {
                TreeSet<Integer> indices_sorted = new TreeSet(indices);
                d_c[c] = indices_sorted.first();
            }
        }
    }

    private static void compute__f_c_s__nw_c_s() {
        for(int c = 0; c < c_info.length; c++) {
            int prio_c = c_info[c][2];
            for(int s = 0; s < stacks; s++) {
                int containers_above_ca = 0;
                int containers_w_above_ca = 0;
                for (int t = tiers-1; t >= 0; t--) {
                    if (copy[s][t][2] == 1 && copy[s][t][1] >= prio_c) {
                        f_c_s[c][s] = containers_above_ca;
                        nw_c_s[c][s] = containers_w_above_ca;
                        break;
                    } else if (copy[s][t][1] != 0) {
                        containers_above_ca++;
                        if (copy[s][t][2] == 1){
                            containers_w_above_ca++;
                        }
                        if (t == 0) {
                            f_c_s[c][s] = containers_above_ca;
                            nw_c_s[c][s] = containers_w_above_ca;
                        }
                    }
                }
            }
        }
    }

    private static void compute_bay_info() {
        int empty_stacks_count = 0;
        for (int s = 0; s < stacks; s++) {
            if (s_info[s][1] == 1) {
                if (empty_stacks_count < 2) {
                    bay_info[3 + empty_stacks_count][0] = s;
                    bay_info[3 + empty_stacks_count][1] = -1;
                    empty_stacks_count++;
                } else {
                    //wenn zwei leere Stacks gefunden, kann es keine forced relocations geben -> fr_c_s dann uninteressant
                    break;
                }
            }
            else  {
                if (copy[s][s_info[s][0]][1] > bay_info[0][0]) {
                    bay_info[2][0] = bay_info[1][0];
                    bay_info[2][1] = bay_info[1][1];
                    bay_info[1][0] = bay_info[0][0];
                    bay_info[1][1] = bay_info[0][1];
                    bay_info[0][0] = copy[s][s_info[s][0]][1];
                    bay_info[0][1] = s;
                } else if (copy[s][s_info[s][0]][1] == bay_info[0][0]) {
                    bay_info[2][0] = bay_info[1][0];
                    bay_info[2][1] = bay_info[1][1];
                    bay_info[1][0] = copy[s][s_info[s][0]][1];
                    bay_info[1][1] = s;
                }else if (copy[s][s_info[s][0]][1] > bay_info[1][0]) {
                    bay_info[2][0] = bay_info[1][0];
                    bay_info[2][1] = bay_info[1][1];
                    bay_info[1][0] = copy[s][s_info[s][0]][1];
                    bay_info[1][1] = s;
                } else if (copy[s][s_info[s][0]][1] > bay_info[2][0]) {
                    bay_info[2][0] = copy[s][s_info[s][0]][1];
                    bay_info[2][1] = s;
                }
            }
        }
    }

    private static void compute__c_info__s_info__g_c_s() {
        for(int s = 0; s < stacks; s++) {
            int containers_above_c = 0;
            boolean highest_tier_found = false;
            boolean tier_ordered = true;
            for (int t = tiers-1; t >= 0; t--) {
                if (copy[s][t][0] != 0) {
                    c_info[copy[s][t][0]-1][0] = s;
                    c_info[copy[s][t][0]-1][1] = t;
                    c_info[copy[s][t][0]-1][2] = copy[s][t][1];
                    c_info[copy[s][t][0]-1][3] = copy[s][t][2];
                    g_c_s[copy[s][t][0]-1][s] = containers_above_c;
                    containers_above_c++;
                    if (!highest_tier_found) {
                        s_info[s][0] = t;
                        highest_tier_found = true;
                    }
                    if (tier_ordered) {
                        if (copy[s][t][2] == 0) {
                            tier_ordered = false;
                        }
                    }
                }
            }
            if(!highest_tier_found) {
                s_info[s][0] = -1;
            }
            s_info[s][1] = tier_ordered ? 1 : 0;
        }
    }

    private static int[][][] copy_bay(int[][][] initial_bay, int[][][] current_bay) {
        for(int s = 0; s < stacks; s++) {
            for (int t = 0; t < tiers; t++) {
                for (int i = 0; i < 3; i++) {
                    current_bay[s][t][i] = initial_bay[s][t][i];
                }
            }
        }
        return current_bay;
    }

    private static void compute_if_well_located(int [][][] bay) {
        for(int s = 0; s < stacks; s++) {
            for(int t = 0; t < tiers; t++) {
                if (t == 0) {
                    bay[s][t][2] = 1;
                } else if(bay[s][t][1] > bay[s][t-1][1] || bay[s][t-1][2] == 0) {
                    bay[s][t][2] = 0;
                } else if(bay[s][t][1] <= bay[s][t-1][1] && bay[s][t-1][2] == 1) {
                    bay[s][t][2] = 1;
                }
            }
        }
    }

    private static void relocate(int block, int prio, int next_stack) {
        boolean next_stack_sorted = (s_info[next_stack][1] == 1);
        int next_tier = s_info[next_stack][0] + 1;
        int prio_next_stack;
        if (next_tier != 0) {
            prio_next_stack = copy[next_stack][next_tier-1][1];
        } else {
            prio_next_stack = 100000;
        }
        int prev_stack = c_info[block][0];
        int prev_tier = c_info[block][1];
        boolean prev_stack_sorted;
        if (prev_tier != 0) {
            prev_stack_sorted = (copy[prev_stack][prev_tier-1][2] == 1);
        } else {
            prev_stack_sorted = true;
        }

        System.out.println("Block " + block + " from stack " + prev_stack + " to stack " + next_stack);
        if (prev_tier != tiers-1 && copy[prev_stack][prev_tier+1][0] != 0) {
            System.out.println("Illegal move!");
            System.exit(0);
        }

        copy[next_stack][next_tier][0] = block + 1;
        copy[next_stack][next_tier][1] = prio;
        if (next_stack_sorted && next_tier != 0) {
            if (prio > prio_next_stack) {
                copy[next_stack][next_tier][2] = 0;
                s_info[next_stack][1] = 0;
                for (int t = next_tier + 1; t < tiers; t++) {
                    copy[next_stack][t][2] = 0;
                }
            }
        }
        s_info[next_stack][0] += 1;
        copy[prev_stack][prev_tier][0] = 0;
        copy[prev_stack][prev_tier][1] = 0;
        s_info[prev_stack][0] -= 1;
        if (copy[prev_stack][prev_tier][2] == 0) {
            if (prev_stack_sorted) {
                s_info[prev_stack][1] = 1;
                for (int t = prev_tier; t < tiers; t++) {
                    copy[prev_stack][t][2] = 1;
                }
            }
        }
        c_info[block][0] = next_stack;
        c_info[block][1] = s_info[next_stack][0];
        c_info[block][3] = s_info[next_stack][1];

        relocations++;
        distance_relocations[0] += (tiers - prev_tier) + (tiers - next_tier);
        distance_relocations[1] += Math.abs(next_stack - prev_stack);

        System.out.println("current_bay: " + Arrays.deepToString(copy));
        System.out.println("c_info: " + Arrays.deepToString(c_info));
        System.out.println("s_info: " + Arrays.deepToString(s_info));
    }

    private static void get_initial_bay(String initial_bay_path) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(initial_bay_path));

        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.next();
        tiers = scanner.nextInt();
        System.out.println("Tiers: " + tiers);
        scanner.next();
        stacks = scanner.nextInt();
        System.out.println("Stacks: " + stacks);
        scanner.next();
        containers = scanner.nextInt();
        System.out.println("Containers: " + containers);
        scanner.nextLine();

        initial_bay = new int[stacks][tiers][3]; //3-te Dimension: Nummer, Prio, well-located
        int number_container = 1;
        for(int i=0; i < stacks; i++) {
            String str = scanner.nextLine();
            //System.out.println("nextLine: " + str);
            str = str.replaceAll("[^-?0-9]+", " ");
            String [] help = (str.trim().split(" "));
            //System.out.println(Arrays.toString(help));
            int j = 0;
            while(j < help.length-1){
                //priorities werden um jeweils 1 erhöht, damit leere felder den eintrag 0 erhalten können
                initial_bay[i][j][0] = number_container;
                initial_bay[i][j][1] = Integer.parseInt(help[j+1]) + 1;
                //System.out.println(initial_bay[i][j]);
                j++;
                number_container++;
            }
        }
    }

}
