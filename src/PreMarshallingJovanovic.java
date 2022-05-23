import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PreMarshallingJovanovic {

    static boolean consider_time;
    static boolean multiple_bays;

    static int [][][] current_bay;

    static int [][] bay_info = new int [5][2]; //1st/2nd and 3rd highest top stack due date value of not well located stacks (prio and stack) and -1 at position 4 and/or 5 if there are empyty stacks
    static int [][] s_info; //highest tier, ordered
    static int [][] c_info; //stack, tier, prio, sorted
    static boolean sorted;

    static int [][]  g_c_s; //number of containers above c in stack s
    static int [][] f_c_s; //containers above well located block c_a with larger or same due date value than c in destination stack s
    static int [][]nw_c_s; //well located containers that need to be relocated when c is moved to destination stack s
    static int [][] f_c_s_ext; //berücksichtigt zusätzlich zu f_c_s, dass Umlagerungen von bereits richtig platzierten Blöcken vermieden werden sollten
    static int [][] w_c_s; //Summe der Blöcke, die bei einer Umlagerung von c aus stack s in destination stack ss, aus s und ss heraus umgelagert werden müssen
    static int [] d_c; //d_c is the index of the stack that the lowest value of w_c_s for a block c and if tied the minimum distance to c

    static TreeSet<Relocation> relocations = new TreeSet<>(); //relocation_count, block, prev_stack, next_stack, prev_tier, next_tier
    static TreeSet<Relocation> relocations_on_hold;

    static String next_selection;
    static String stack_selection;
    static String stack_filling;


    static int step = 0;
    static double time_relocations = 0; //keine Leerfahrten berücksichtigt, ohne Lastaufnahme und -abgabe
    static double time_total = 0; //ohne Lastaufnahme und -abgabe


    public static void main (String [] args) throws FileNotFoundException {
        String initial_bay_path = "/Users/luismasuchibanez/IdeaProjects/premarshalling_heuristics/data/Test/emm_s10_t4_p1_c0_16.bay";
        consider_time = true;
        multiple_bays = true;
        next_selection = "function h_c"; //"highest due date value"
        //TODO: certain improvements can be achieved by adding some fine tuning (stack_selection)
        stack_selection = "The Lowest Position";//"The Lowest Position", "Lowest Priority Index", "MinMax"
        stack_filling = "None"; //"None", "Standard", "Safe", "Stop"
        try {
            BayInstance instance = get_initial_bay(initial_bay_path);
            if (multiple_bays) {
                int [][][] initial_bay = instance.initial_bay;
                int stacks = instance.stacks;
                int stacks_per_bay = instance.stacks_per_bay;
                int tiers = instance.tiers;
                int containers = instance.containers;

                //check if blocks are well located or not (1:well located, 0: not well located)
                compute_if_well_located(instance.initial_bay, stacks, tiers);
                System.out.println("Initial bay: " + Arrays.deepToString(instance.initial_bay));
                check_sorted_pre(initial_bay, stacks, tiers);

                int[][][] final_bay = premarshall(initial_bay, stacks, stacks_per_bay, tiers, containers, Relocation.order_relocations, Relocation.same_stack_under, Relocation.same_stack_below);
                time_relocations = (((double) Relocation.distance_relocations[0] * 2 * 15) + ((double) Relocation.distance_relocations[1] * 2 * 2.4) + ((double) Relocation.distance_relocations[2] * 2 * 1.875)) / 3600;
                time_total = (((double) Relocation.distance_total[0] * 2 * 15) + ((double) Relocation.distance_total[1] * 2 * 2.4) + ((double) Relocation.distance_total[2] * 2 * 1.875) + Relocation.relocations_count * 2 * 20.0) / 3600;

                System.out.println("Final bay: " + Arrays.deepToString(final_bay));
                System.out.println("Relocations: " + Relocation.relocations_count);
                System.out.println("Distance_relocations in blocks: " + Arrays.toString(Relocation.distance_relocations));
                System.out.println("Distance_total in blocks: " + Arrays.toString(Relocation.distance_total));
                System.out.println("Time_relocations in h: " + time_relocations);
                System.out.println("Time_total in h: " + time_total);
                if (Relocation.deadlock_count > 0) {
                    System.out.println("Deadlocks: " + Relocation.deadlock_count);
                }
                /*
                Iterator<int[]> it = relocations.iterator();
                while(it.hasNext()){
                    System.out.println(it.next()[0] + " " + it.next()[1]);
                }
                 */
            } else {
                int stacks = instance.stacks_per_bay;
                int stacks_per_bay = instance.stacks_per_bay;
                int tiers = instance.tiers;
                for (int b = 0; b < instance.bays; b++) {
                    step = 0;
                    int containers = instance.containers_per_bay[b];
                    int initial_bay [][][] = instance.initial_bays[b];
                    //check if blocks are well located or not (1:well located, 0: not well located)
                    compute_if_well_located(initial_bay, stacks, tiers);
                    System.out.println("Initial bay: " + Arrays.deepToString(initial_bay));
                    check_sorted_pre(initial_bay, stacks, tiers);

                    int[][][] final_bay = premarshall(initial_bay, stacks, stacks_per_bay, tiers, containers, Relocation.order_relocations, Relocation.same_stack_under, Relocation.same_stack_below);

                    System.out.println("Final bay: " + Arrays.deepToString(final_bay));
                    System.out.println("Relocations: " + Relocation.relocations_count);
                    System.out.println("Distance_relocations in blocks: " + Arrays.toString(Relocation.distance_relocations));
                    System.out.println("Time_relocations in h: " + time_relocations);
                    System.out.println("Distance_total in blocks: " + Arrays.toString(Relocation.distance_total));
                    System.out.println("Time_total in h: " + time_total);
                    if (Relocation.deadlock_count > 0) {
                        System.out.println("Deadlocks: " + Relocation.deadlock_count);
                    }
                }
                time_relocations += (((double) Relocation.distance_relocations[0] * 2 * 15) + ((double) Relocation.distance_relocations[1] * 2 * 2.4)) / 3600;
                time_total += (((double) Relocation.distance_total[0] * 2 * 15) + ((double) Relocation.distance_total[1] * 2 * 2.4) + Relocation.relocations_count * 2 * 20.0) / 3600;
                System.out.println("Time_relocations in h: " + time_relocations);
                System.out.println("Time_total in h: " + time_total);

            }
        } catch (FileNotFoundException e) {
            System.out.println(("File not found!"));
        }
        
    }

    public static void check_sorted_pre(int [][][] initial_bay, int stacks, int tiers) {
        sorted = true;
        for (int s = 0; s < stacks; s++) {
            for (int t = 0; t < tiers; t++) {
                if (initial_bay[s][t][2] == 0) {
                    sorted = false;
                    break;
                }
            }
        }
        if (sorted) {
            System.out.println("Bay ist bereits geordnet!");
        }
    }

    public static int[][][] premarshall(int[][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers, int [] order_relocations, boolean same_stack_under, int [] same_stack_below) {
        current_bay = new int[stacks][tiers][3];
        current_bay = copy_bay(initial_bay, current_bay, stacks, tiers);

        s_info = new int [stacks][2];
        c_info = new int[containers][4];

        Relocation.copy = new int[stacks][tiers][3]; //Kopie, damit current_bay nicht verändert wird, falls Schritt rückgängig gemacht werden muss nach look-ahead

        Relocation.deadlock = false;
        Relocation.deadlock_next = new int[containers];
        while(!sorted) {
            relocations_on_hold = new TreeSet<>();
            step++;
            System.out.println("Step " + step);
            System.out.println("Current bay: " + Arrays.deepToString(current_bay));

            Relocation.copy = copy_bay(current_bay, Relocation.copy, stacks, tiers);
            int [][][] copy = Relocation.getCopy();

            g_c_s = new int[containers][stacks];
            compute__c_info__s_info__g_c_s(copy, stacks, tiers);

            //calculating bay_info
            compute_bay_info(copy, stacks);

            System.out.println("c_info: " + Arrays.deepToString(c_info));
            System.out.println("s_info: " + Arrays.deepToString(s_info));
            //System.out.println("bay_info: " + Arrays.deepToString(bay_info));
            //System.out.println("g_c_s: " + Arrays.deepToString(g_c_s));

            //TODO: wirklich larger or same due date value?
            f_c_s = new int[containers][stacks];
            nw_c_s = new int[containers][stacks];
            compute__f_c_s__nw_c_s(copy, stacks, tiers);
            //System.out.println("f_c_s: " + Arrays.deepToString(f_c_s));
            //System.out.println("nw_c_s: " + Arrays.deepToString(nw_c_s));

            f_c_s_ext = new int[containers][stacks];
            compute__f_c_s_ext(stacks, containers);
            //System.out.println("f_c_s_ext: " + Arrays.deepToString(f_c_s_ext));

            w_c_s = new int[containers][stacks];
            compute__w_c_s(stacks, containers);
            //System.out.println("w_c_s: " + Arrays.deepToString(w_c_s));

            d_c = new int [containers];
            compute_d_c(copy, stacks, stacks_per_bay, tiers, containers);
            System.out.println("d_c: " + Arrays.toString(d_c));

            Relocation.get_next(c_info, s_info, d_c, w_c_s, f_c_s, next_selection, consider_time, containers, tiers, stacks_per_bay);
            int next = Relocation.next;
            System.out.println("next: " + next + " from stack " + c_info[next][0] + " to stack " + d_c[next]);

            Relocation.same_stack = false;
            Relocation.same_stack_over = false;
            Relocation.same_stack_under = false;
            Relocation.next_to_stopover_stack_prevent_deadlock = false;

            Relocation.compute__order_relocations__same_stack_below(c_info, s_info, d_c, g_c_s, f_c_s, next);
            System.out.println("order_relocations: " + Arrays.toString(order_relocations));
            if (same_stack_under) {
                System.out.println("same_stack_below: " + Arrays.toString(same_stack_below));
            }

            //die Blöcke umlagern, die next blockieren (egal welcher stack) oder next's Platz im destination stack blockieren (wenn stack gleich destination stack)
            Relocation.relocate_blocking_blocks(c_info, s_info, d_c, stack_selection, consider_time, stacks, stacks_per_bay, tiers);

            //next umlagern
            if (!Relocation.deadlock) {
                Relocation.deadlock_next = new int[containers];
                Relocation.relocate_next(c_info, s_info, d_c, stack_selection, consider_time, stacks, tiers, stacks_per_bay);
            } else {
                System.out.println("DEADLOCK!");
                Relocation.deadlock_next[next] = next+1;
                System.out.println("deadlock_next: " + Arrays.toString(Relocation.deadlock_next));
                Relocation.deadlock_count++;
                Relocation.copy = copy_bay(current_bay, copy, stacks, tiers);
            }

            //filling
            if (!Relocation.deadlock) {
                stack_filling(copy, stacks, stacks_per_bay, tiers, next);
                relocations.addAll(relocations_on_hold);
            }

            current_bay = copy_bay(Relocation.copy, current_bay, stacks, tiers);

            check_sorted(stacks);
        }
        //TODO: Corrections -> auf Set relocations durchführen, kann wohl bei filling auftreten, sonst eher selten
        return current_bay;
    }

    public static void stack_filling(int [][][] copy, int stacks, int stacks_per_bay, int tiers, int next) {
        int next_stack = c_info[next][0];
        int next_prio = c_info[next][2];
        int height = s_info[c_info[next][0]][0];
        int current_height = height;
        int candidate_block;
        int candidate_stack;
        int candidate_prio;
        boolean candidate_found = false;
        if (stack_filling == "Standard") {
            while (current_height < tiers - 1) {
                candidate_found = false;
                candidate_block = 0;
                candidate_prio = 0;
                for (int s = 0; s < stacks; s++) {
                    if (s != next_stack && s_info[s][0] != -1) {
                        if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] < next_prio) {
                            int candidate_prio_option = copy[s][s_info[s][0]][1];
                            if (candidate_prio_option > candidate_prio) {
                                candidate_block = copy[s][s_info[s][0]][0] - 1;
                                candidate_found = true;
                                candidate_prio = candidate_prio_option;
                            }
                        }
                    }
                }
                //TODO: hier consider_time?
                if (candidate_found) {
                    System.out.println("Stack filling!");
                    Relocation.relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay);
                    current_height++;
                } else {
                    current_height = tiers;
                }
            }
        } else if (stack_filling == "Safe") {
            int [][] filling_blocks = new int [tiers-1][2]; //block, prio
            int filling_count = 0;
            int [][] s_info_help = new int [stacks][2];
            for (int s = 0; s < stacks; s++) {
                s_info_help[s][0] = s_info[s][0];
                s_info_help[s][1] = s_info[s][1];
            }
            while (current_height < tiers - 1) {
                candidate_found = false;
                candidate_block = 0;
                candidate_stack = 0;
                candidate_prio = 0;
                for (int s = 0; s < stacks; s++) {
                    if (s != next_stack && s_info_help[s][0] != -1) {
                        if (s_info_help[s][1] == 0 && copy[s][s_info_help[s][0]][1] < next_prio) {
                            int candidate_prio_option = copy[s][s_info_help[s][0]][1];
                            if (candidate_prio_option > candidate_prio) {
                                candidate_block = copy[s][s_info_help[s][0]][0] - 1;
                                candidate_stack = s;
                                candidate_found = true;
                                candidate_prio = candidate_prio_option;
                            }
                        }
                    }
                }
                //TODO: hier consider_time?
                if (candidate_found) {
                    filling_blocks[filling_count][0] = candidate_block;
                    filling_blocks[filling_count][1] = candidate_prio;
                    s_info_help[candidate_stack][0] -= 1;
                    s_info_help[candidate_stack][1] = copy[candidate_stack][c_info[candidate_block][1]-1][2];
                    current_height++;
                    filling_count++;
                } else {
                    current_height = tiers;
                }
            }
            int alpha = 1;
            if ((tiers - (height + 1 + filling_count)) <= alpha) {
                for (int c = 0; c < filling_blocks.length; c++) {
                    if (filling_blocks[c][1] != 0) {
                        System.out.println("Stack filling!");
                        Relocation.relocate(c_info, s_info, filling_blocks[c][0], filling_blocks[c][1], next_stack, tiers, stacks_per_bay);
                    }
                }
            }
        } else if (stack_filling == "Stop") {
            while (current_height < tiers - 1) {
                candidate_found = false;
                candidate_block = 0;
                candidate_prio = 0;
                for (int s = 0; s < stacks; s++) {
                    if (s != next_stack && s_info[s][0] != -1) {
                        if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] < next_prio) {
                            int candidate_prio_option = copy[s][s_info[s][0]][1];
                            if (candidate_prio_option > candidate_prio) {
                                candidate_block = copy[s][s_info[s][0]][0] - 1;
                                candidate_found = true;
                                candidate_prio = candidate_prio_option;
                            }
                        }
                    }
                }
                //TODO: hier consider_time?
                if (candidate_found) {
                    //wenn unter ausgewähltem Block ein Block
                    if (s_info[c_info[candidate_block][0]][0] > 0 && candidate_prio < copy[c_info[candidate_block][0]][c_info[candidate_block][1]-1][2]) {
                        current_height = tiers;
                    } else {
                        System.out.println("Stack filling!");
                        Relocation.relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay);
                        current_height++;
                    }
                } else {
                    current_height = tiers;
                }
            }
        }
    }

    public static void check_sorted(int stacks) {
        sorted = true;
        for (int s = 0; s < stacks; s++) {
            if (s_info[s][1] == 0) {
                sorted = false;
            }
        }
    }

    public static void compute__w_c_s(int stacks, int containers) {
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

    public static void compute__f_c_s_ext(int stacks, int containers) {
        for (int c = 0; c < containers; c++) {
            for (int s = 0; s < stacks; s++) {
                f_c_s_ext[c][s] = f_c_s[c][s] + nw_c_s[c][s];
            }
        }
    }

    public static int[] compute_fr_c_s(int [][][] copy, int[] fr_c_s, int[] d_c, Object[] next_options) {
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

    public static void compute_d_c(int [][][] copy, int stacks, int stacks_per_bay, int tiers, int containers) {
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
                    double time_to_current_stack = (double) Math.abs(c_info[c][0]/stacks_per_bay - indice/stacks_per_bay) + (double) Math.abs(c_info[c][0] % stacks_per_bay - indice % stacks_per_bay) * 2.4 + (double) ((tiers - c_info[c][1]) + (tiers - (s_info[indice][0] - f_c_s[c][indice]))) * 15.0;
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

    public static void compute__f_c_s__nw_c_s(int [][][] copy, int stacks, int tiers) {
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

    public static void compute_bay_info(int [][][] copy, int stacks) {
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

    public static void compute__c_info__s_info__g_c_s(int [][][] copy, int stacks, int tiers) {
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

    public static int[][][] copy_bay(int[][][] initial_bay, int[][][] current_bay, int stacks, int tiers) {
        for(int s = 0; s < stacks; s++) {
            for (int t = 0; t < tiers; t++) {
                for (int i = 0; i < 3; i++) {
                    current_bay[s][t][i] = initial_bay[s][t][i];
                }
            }
        }
        return current_bay;
    }

    public static void compute_if_well_located(int [][][] bay, int stacks, int tiers) {
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

    public static BayInstance get_initial_bay(String initial_bay_path) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(initial_bay_path));

        if (multiple_bays) {
            scanner.nextLine();
            scanner.nextLine();
            scanner.nextLine();
            scanner.next();
            int bays = scanner.nextInt();
            System.out.println("Bays: " + bays);
            scanner.next();
            int tiers = scanner.nextInt();
            System.out.println("Tiers: " + tiers);
            scanner.next();
            int stacks = scanner.nextInt();
            int stacks_per_bay = stacks / bays;
            System.out.println("Stacks: " + stacks);
            scanner.next();
            int containers = scanner.nextInt();
            System.out.println("Containers: " + containers);
            scanner.nextLine();

            int [][][] initial_bay = new int[stacks][tiers][3]; //3-te Dimension: Nummer, Prio, well-located
            int number_container = 1;
            for (int s = 0; s < stacks; s++) {
                String str = scanner.nextLine();
                str = str.replaceAll("[^-?0-9]+", " ");
                String[] help = (str.trim().split(" "));
                for (int t = 0; t < help.length - 1; t++) {
                    //priorities werden um jeweils 1 erhöht, damit leere felder den eintrag 0 erhalten können
                    initial_bay[s][t][0] = number_container;
                    initial_bay[s][t][1] = Integer.parseInt(help[t + 1]) + 1;
                    number_container++;
                }
            }
            return new BayInstance(bays, stacks, stacks_per_bay, tiers, containers, initial_bay);
        } else {
            scanner.nextLine();
            scanner.nextLine();
            scanner.nextLine();
            scanner.next();
            int bays = scanner.nextInt();
            System.out.println("Bays: " + bays);
            scanner.next();
            int tiers = scanner.nextInt();
            System.out.println("Tiers: " + tiers);
            scanner.next();
            int stacks = scanner.nextInt();
            int stacks_per_bay = stacks / bays;
            System.out.println("Stacks: " + stacks);
            scanner.next();
            int containers = scanner.nextInt();
            System.out.println("Containers: " + containers);
            scanner.nextLine();

            int [][][][] initial_bays = new int[bays][][][]; //3-te Dimension: Nummer, Prio, well-located
            int [] containers_per_bay = new int[bays];
            for (int b= 0; b < bays; b++) {
                int number_container = 1;
                int [][][] initial_bay = new int[stacks/bays][tiers][3]; //3-te Dimension: Nummer, Prio, well-located
                for (int s = 0; s < stacks/bays; s++) {
                    String str = scanner.nextLine();
                    str = str.replaceAll("[^-?0-9]+", " ");
                    String[] help = (str.trim().split(" "));
                    for (int t = 0; t < help.length - 1; t++) {
                        //priorities werden um jeweils 1 erhöht, damit leere felder den eintrag 0 erhalten können
                        initial_bay[s][t][0] = number_container;
                        initial_bay[s][t][1] = Integer.parseInt(help[t + 1]) + 1;
                        number_container++;
                    }
                }
                initial_bays[b] = initial_bay;
                containers_per_bay[b] = number_container-1;
            }
            return new BayInstance(bays, stacks_per_bay, tiers, containers_per_bay, initial_bays);
        }
    }
}
