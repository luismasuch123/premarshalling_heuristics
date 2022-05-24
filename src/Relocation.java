import java.util.Arrays;
import java.util.TreeSet;

public class Relocation implements Comparable<Relocation> {
    int relocation_count;
    int block;
    int prev_stack;
    int next_stack;
    int prev_tier;
    int next_tier;

    static int [][][] copy;
    static int current_bay;

    //Jovanovic
    static boolean same_stack;
    static boolean same_stack_over;
    static boolean same_stack_under;
    static boolean next_to_stopover_stack_prevent_deadlock;
    static int stopover_stack_prevent_deadlock;
    static boolean deadlock = false;
    static int deadlock_count;
    static int [] deadlock_next;
    static int [] order_relocations;
    static int [] selected_stacks;
    static int [] same_stack_below;

    static int next_block;
    static int prev_block; //letzter Block der umgelagert wurde

    static int relocations_count = 0;
    static int [] distance_relocations = new int[3]; //tiers, stacks, bays in blocks
    static int [] distance_total = new int[3];

    Relocation(int relocation_count, int block, int prev_stack, int next_stack, int prev_tier, int next_tier) {
        this.relocation_count = relocation_count;
        this.block = block;
        this.prev_stack = prev_stack;
        this.next_stack = next_stack;
        this.prev_tier = prev_tier;
        this.next_tier = next_tier;
    }

    @Override
    public int compareTo(Relocation r) {
        return Integer.compare(relocation_count, r.relocation_count);
        //return this.relocation_count >= r.relocation_count ? -1 : 0;
    }

    @Override
    public String toString() {

        return "[" + relocation_count + ", " + block + ", " + prev_stack + ", " + next_stack + ", " + prev_tier + ", " + next_tier + "]";
    }

    public static void relocate_next_Jovanovic(int [][] c_info, int [][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays, int stacks, int tiers, int stacks_per_bay) {
        if (next_to_stopover_stack_prevent_deadlock) {
            int block = next_block;
            int prio = c_info[next_block][2];
            int next_stack = stopover_stack_prevent_deadlock;

            relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            next_to_stopover_stack_prevent_deadlock = false;
        } else if (!same_stack_under) {
            int block = next_block;
            int prio = c_info[next_block][2];
            int next_stack = d_c[next_block];

            relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
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
                } else if (stack_selection == "Lowest Priority Index") {
                    int highest_due_date_value = 0;
                    for (int s = 0; s < stacks; s++) {
                        int highest_due_date_value_option = 0;
                        if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                            if (! ((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
                                if (s_info_help[s][1] == 0) {
                                    for (int t = 0; t <= s_info[s][0]; t++) {
                                        if (copy[s][t][2] == 0){
                                            if (copy[s][t][1] > highest_due_date_value_option) {
                                                highest_due_date_value_option = copy[s][t][1];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (highest_due_date_value_option > highest_due_date_value) {
                            highest_due_date_value = highest_due_date_value_option;
                            stack_options.clear();
                            stack_options.add(s);
                        } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                            stack_options.add(s);
                        }
                    }
                    //wenn kein not well located block für relocation vorhanden, dann wird relocation auf Stack mit der niedrigsten, höchsten Prio innerhalb des Stacks durchgeführt
                    if (stack_options.size() == 0) {
                        int lowest_due_date_value = 1000000;
                        for (int s = 0; s < stacks; s++) {
                            int highest_due_date_value_option = 0;
                            if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                                if (! ((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
                                    if (s_info_help[s][1] == 1) {
                                        for (int t = 0; t <= s_info[s][0]; t++) {
                                            if (copy[s][t][1] > highest_due_date_value_option) {
                                                highest_due_date_value_option = copy[s][t][1];
                                            }
                                        }
                                    }
                                }
                            }
                            if (highest_due_date_value_option != 0 && highest_due_date_value_option < lowest_due_date_value) {
                                lowest_due_date_value = highest_due_date_value_option;
                                stack_options.clear();
                                stack_options.add(s);
                            } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                                stack_options.add(s);
                            }
                        }
                    }
                } else if (stack_selection == "MinMax") {
                    int highest_due_date_value = 0;
                    for (int s = 0; s < stacks; s++) {
                        int highest_due_date_value_option = 0;
                        if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                            if (! ((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
                                for (int t = 0; t <= s_info[s][0]; t++) {
                                    if (copy[s][t][1] > highest_due_date_value_option) {
                                        highest_due_date_value_option = copy[s][t][1];
                                    }
                                }
                            }
                        }
                        if (highest_due_date_value_option > highest_due_date_value) {
                            highest_due_date_value = highest_due_date_value_option;
                            stack_options.clear();
                            stack_options.add(s);
                        } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                            stack_options.add(s);
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
                        double time_to_destination_stack_option = (double) Math.abs(c_info[same_stack_below[c]][0]/stacks_per_bay - stack/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[same_stack_below[c]][0] % stacks_per_bay - stack % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[same_stack_below[c]][1]) + (tiers - (s_info_help[stack][0]))) * 2 * 15.0;
                        if (time_to_destination_stack_option < time_to_destination_stack) {
                            selected_stack[c] = stack;
                        }
                    }
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
                    if (!selected_stacks_contains_s && s != d_c[next_block] && s_info[s][0] < tiers - 1) {
                        if (consider_time) {
                            double time_to_stopover_stack_option = (double) Math.abs(c_info[next_block][0]/stacks_per_bay - s/stacks_per_bay) * 2 * 1.875+ (double) Math.abs(c_info[next_block][0] % stacks_per_bay - s % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next_block][1]) + (tiers - (s_info[s][0]))) * 2 * 15.0;
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
                int block = next_block;
                int prio = c_info[block][2];
                int next_stack = stopover_stack;

                relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);

                //jeweils Block c aus same_stack_below[c] in selected_stack[c] umlagern
                for (int c = 0; c < same_stack_below.length; c++) {
                    block = same_stack_below[c];
                    prio = c_info[block][2];
                    next_stack = selected_stack[c];

                    relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                }

                //next von stopover_stack in ds umlagern
                block = next_block;
                prio = c_info[block][2];
                next_stack = d_c[next_block];

                relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            }
        }
    }

    public static void relocate_blocking_blocks_Jovanovic(int [][] c_info, int [][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays, int stacks, int stacks_per_bay, int tiers) {
        get_selected_stacks_Jovanovic(c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);

        if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
            //relocation durchführen in order_relocation und Updates
            for (int c = 0; c < order_relocations.length; c++) {
                int block = order_relocations[c];
                int prio = c_info[block][2];
                int next_stack = selected_stacks[c];

                relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            }
        } else if (next_to_stopover_stack_prevent_deadlock && !deadlock) {
            relocate_next_Jovanovic(c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
            get_selected_stacks_Jovanovic(c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);
            if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
                //relocation durchführen in order_relocation und Updates
                for (int c = 0; c < order_relocations.length; c++) {
                    int block = order_relocations[c];
                    int prio = c_info[block][2];
                    int next_stack = selected_stacks[c];

                    relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                }
            }
        }
    }

    public static void relocate(int [][] c_info, int [][] s_info, int block, int prio, int next_stack, int tiers, int stacks_per_bay, boolean multiple_bays) {
        //TODO: erste Fahrt berücksichtitgen und bei multiple_bays=false Fahrten zwischen bays
        distance_total[0] += (tiers - c_info[prev_block][1]) + (tiers - c_info[block][1]);
        distance_total[1] += Math.abs(c_info[prev_block][0] % stacks_per_bay - c_info[block][0] % stacks_per_bay);
        distance_total[2] += Math.abs(c_info[prev_block][0] /stacks_per_bay - c_info[block][0]/ stacks_per_bay);

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
                Params.s_info[next_stack][1] = 0;
                for (int t = next_tier + 1; t < tiers; t++) {
                    copy[next_stack][t][2] = 0;
                }
            }
        }
        Params.s_info[next_stack][0] += 1;
        copy[prev_stack][prev_tier][0] = 0;
        copy[prev_stack][prev_tier][1] = 0;
        Params.s_info[prev_stack][0] -= 1;
        if (copy[prev_stack][prev_tier][2] == 0) {
            if (prev_stack_sorted) {
                Params.s_info[prev_stack][1] = 1;
                for (int t = prev_tier; t < tiers; t++) {
                    copy[prev_stack][t][2] = 1;
                }
            }
        }
        Params.c_info[block][0] = next_stack;
        Params.c_info[block][1] = s_info[next_stack][0];
        Params.c_info[block][3] = s_info[next_stack][1];

        prev_block = block;

        relocations_count++;
        distance_relocations[0] += (tiers - prev_tier) + (tiers - next_tier);
        distance_relocations[1] += Math.abs(next_stack % stacks_per_bay - prev_stack % stacks_per_bay);
        distance_relocations[2] += Math.abs(next_stack /stacks_per_bay - prev_stack/ stacks_per_bay);
        distance_total[0] += (tiers - prev_tier) + (tiers - next_tier);
        distance_total[1] += Math.abs(next_stack % stacks_per_bay - prev_stack % stacks_per_bay);
        distance_total[2] += Math.abs(next_stack /stacks_per_bay - prev_stack/ stacks_per_bay);

        if (multiple_bays) {
            PreMarshallingJovanovic.relocations_on_hold.add(new Relocation(relocations_count, block, prev_stack, next_stack, prev_tier, next_tier));
        } else {
            PreMarshallingJovanovic.relocations_on_hold.add(new Relocation(relocations_count, block, prev_stack + current_bay * stacks_per_bay, next_stack + current_bay * stacks_per_bay, prev_tier, next_tier));
        }

        System.out.println("current_bay: " + Arrays.deepToString(copy));
        System.out.println("c_info: " + Arrays.deepToString(Params.c_info));
        System.out.println("s_info: " + Arrays.deepToString(Params.s_info));
    }

    public static void compute__order_relocations__same_stack_below_Jovanovic(int[][] c_info, int[][] s_info, int[] d_c, int [][] g_c_s, int [][] f_c_s, int next) {
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

    public static void get_selected_stacks_Jovanovic(int[][] c_info, int[][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays,  int stacks, int stacks_per_bay, int tiers) {
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
                        if (s != c_info[next_block][0] && s != c_info[order_relocations[c]][0] && s != d_c[next_block] && (s_info_help[s][0] + 1) < tiers) {
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
            } else if (stack_selection == "Lowest Priority Index") {
                int highest_due_date_value = 0;
                for (int s = 0; s < stacks; s++) {
                    int highest_due_date_value_option = 0;
                    if (!(same_stack_over && s == c_info[order_relocations[c]][0])) {
                        if (s != c_info[next_block][0] && s != c_info[order_relocations[c]][0] && s != d_c[next_block] && (s_info_help[s][0] + 1) < tiers) {
                            if (s_info_help[s][1] == 0) {
                                for (int t = 0; t <= s_info[s][0]; t++) {
                                    if (copy[s][t][2] == 0) {
                                        if (copy[s][t][1] > highest_due_date_value_option) {
                                            highest_due_date_value_option = copy[s][t][1];
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (highest_due_date_value_option > highest_due_date_value) {
                        highest_due_date_value = highest_due_date_value_option;
                        stack_options.clear();
                        stack_options.add(s);
                    } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                        stack_options.add(s);
                    }
                }
                //wenn kein not well located block für relocation vorhanden, dann wird relocation auf Stack mit der niedrigsten, höchsten Prio innerhalb des Stacks durchgeführt
                if (stack_options.size() == 0) {
                    int lowest_due_date_value = 1000000;
                    for (int s = 0; s < stacks; s++) {
                        int highest_due_date_value_option = 0;
                        if (!(same_stack_over && s == c_info[order_relocations[c]][0])) {
                            if (s != c_info[next_block][0] && s != c_info[order_relocations[c]][0] && s != d_c[next_block] && (s_info_help[s][0] + 1) < tiers) {
                                if (s_info_help[s][1] == 1) {
                                    for (int t = 0; t <= s_info[s][0]; t++) {
                                        if (copy[s][t][1] > highest_due_date_value_option) {
                                            highest_due_date_value_option = copy[s][t][1];
                                        }
                                    }
                                }
                            }
                        }
                        if (highest_due_date_value_option != 0 && highest_due_date_value_option < lowest_due_date_value) {
                            lowest_due_date_value = highest_due_date_value_option;
                            stack_options.clear();
                            stack_options.add(s);
                        } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                            stack_options.add(s);
                        }
                    }
                }
            } else if (stack_selection == "MinMax") {
                int highest_due_date_value = 0;
                for (int s = 0; s < stacks; s++) {
                    int highest_due_date_value_option = 0;
                    if (!(same_stack_over && s == c_info[order_relocations[c]][0])) {
                        if (s != c_info[next_block][0] && s != c_info[order_relocations[c]][0] && s != d_c[next_block] && (s_info_help[s][0] + 1) < tiers) {
                            for (int t = 0; t <= s_info[s][0]; t++) {
                                if (copy[s][t][1] > highest_due_date_value_option) {
                                    highest_due_date_value_option = copy[s][t][1];
                                }
                            }
                        }
                    }
                    if (highest_due_date_value_option > highest_due_date_value) {
                        highest_due_date_value = highest_due_date_value_option;
                        stack_options.clear();
                        stack_options.add(s);
                    } else if (highest_due_date_value_option != 0 && highest_due_date_value_option == highest_due_date_value) {
                        stack_options.add(s);
                    }
                }
            }
            if (stack_options.size() == 0) {
                //relocate next to stopover_stack first before continuing with relocations
                if (c_info[next_block][1] != tiers-1) {
                    if (copy[c_info[next_block][0]][c_info[next_block][1] + 1][0] == 0) {
                        next_to_stopover_stack_prevent_deadlock = true;
                        boolean stopover_stack_found = false;
                        for (int s = 0; s < stacks; s++) {
                            if (s != d_c[next_block] && s != c_info[next_block][0] && s_info[s][0] != tiers-1) {
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
                        if (s != d_c[next_block] && s != c_info[next_block][0] && s_info[s][0] != tiers-1) {
                            stopover_stack_prevent_deadlock = s;
                            relocate_next_Jovanovic(c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
                            break;
                            //TODO: hier könnte time berücksichtigt werden
                        }
                    }
                }

            } else if (stack_options.size() == 1 || !consider_time) {
                selected_stacks[c] = stack_options.first();
                s_info_help[selected_stacks[c]][0] += 1;
                deadlock = false;
            } else if (consider_time) {
                double time_to_destination_stack = 1000000;
                for (int stack : stack_options) {
                    double time_to_destination_stack_option = (double) Math.abs(c_info[order_relocations[c]][0]/stacks_per_bay - stack/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[order_relocations[c]][0] % stacks_per_bay - stack % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[order_relocations[c]][1]) + (tiers - (s_info_help[stack][0]))) * 2 * 15.0;
                    if (time_to_destination_stack_option < time_to_destination_stack) {
                        selected_stacks[c] = stack;
                    }
                }
                s_info_help[selected_stacks[c]][0] += 1;
                deadlock = false;
            }
        }
    }

    public static void get_next_Jovanovic(int [][] c_info, int [][] s_info, int [] d_c, int [][] w_c_s, int [][] f_c_s, String next_selection, boolean consider_time, int containers, int tiers, int stacks_per_bay) {
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
                next_block = get_next_funtion_h_c_Jovanovic(c_info, s_info, consider_time, next_block, d_c, w_c_s, f_c_s, next_options, next_option_found, tiers, stacks_per_bay, containers);
            } else if (next_selection == "highest due date value") {
                next_block = get_next_highest_due_date_value_Jovanovic(c_info, s_info, consider_time, next_block, d_c, f_c_s, next_options, next_option_found, tiers, stacks_per_bay);
            }
        } else {
            System.out.println("DEADLOCK! No next_option found.");
            System.exit(0);
        }
    }

    public static int get_next_funtion_h_c_Jovanovic(int [][] c_info, int [][] s_info, boolean consider_time, int next, int[] d_c, int[][] w_c_s, int[][] f_c_s, Object[] next_options, boolean next_option_found, int tiers, int stacks_per_bay, int containers) {
        //number of forced relocations: in this approximation, a forced relocation occurs only if a block c is being relocated and all of the top stack due date values are larger than p(c)
        //if all blocks in a stack are well located we shall consider the stack having due date value zero
        int[] fr_c_s = new int[containers];
        //TODO: werden im Paper nur forced relocations aus stack s oder auch aus destination stack ss betrachtet? -> hier werden beide betrachtet (Option mit oder ohne?)
        fr_c_s = Params.compute_fr_c_s(copy, fr_c_s, d_c, next_options);
        //System.out.println("fr_c_s: " + Arrays.toString(fr_c_s));

        int[] h_c = new int[containers];
        for (int cc = 0; cc < next_options.length; cc++) {
            int c = (int) next_options[cc];
            h_c[c] = w_c_s[c][d_c[c]] + fr_c_s[c] - c_info[c][2];
        }
        //System.out.println("h_c: " + Arrays.toString(h_c));

        //indice of next block to be relocated is calculated
        //TODO: can in most cases be calculated by evaluating h_c for a small number of blocks -> kaum Einsparung?!
        int min_value = 1000000;
        double time_to_destination_stack = 1000000;
        double time_from_prev = 1000000;
        if (next_option_found) {
            for (int cc = 0; cc < next_options.length; cc++) {
                int c = (int) next_options[cc];
                if (h_c[c] < min_value) {
                    next = c;
                    min_value = h_c[c];
                    time_from_prev = (double) Math.abs(c_info[next][0]/stacks_per_bay - c_info[prev_block][0]/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next][0] % stacks_per_bay - c_info[prev_block][0] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - s_info[c_info[next][0]][0]) + (tiers - c_info[prev_block][1])) * 2 * 15.0;
                    if (s_info[d_c[next]][0] != -1) {
                        time_to_destination_stack = (double) Math.abs(c_info[next][0] / stacks_per_bay - d_c[next] / stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next][0] % stacks_per_bay - d_c[next] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next][1]) + (tiers - (s_info[d_c[next]][0] - f_c_s[next][d_c[next]] + 1))) * 2 * 15.0;
                    } else {
                        time_to_destination_stack = (double) Math.abs(c_info[next][0] / stacks_per_bay - d_c[next] / stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next][0] % stacks_per_bay - d_c[next] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next][1]) + tiers) * 2 * 15.0;
                    }
                } else if (consider_time && h_c[c] == min_value) {
                    int next_option = c;
                    double time_from_prev_option = (double) Math.abs(c_info[next_option][0]/stacks_per_bay - c_info[prev_block][0]/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next_option][0] % stacks_per_bay - c_info[prev_block][0] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - s_info[c_info[next_option][0]][0]) + (tiers - c_info[prev_block][1])) * 2 * 15.0;
                    double time_to_destination_stack_option;
                    if (s_info[d_c[next_option]][0] != -1) {
                        time_to_destination_stack_option = (double) Math.abs(c_info[next_option][0] / stacks_per_bay - d_c[next_option] / stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next_option][0] % stacks_per_bay - d_c[next_option] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next_option][1]) + (tiers - (s_info[d_c[next_option]][0] - f_c_s[next_option][d_c[next_option]] + 1))) * 2 * 15.0;
                    } else {
                        time_to_destination_stack_option = (double) Math.abs(c_info[next_option][0] / stacks_per_bay - d_c[next_option] / stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next_option][0] % stacks_per_bay - d_c[next_option] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next_option][1]) + tiers) * 2 * 15.0;
                    }
                    if (time_to_destination_stack_option < time_to_destination_stack) {
                        time_to_destination_stack = time_to_destination_stack_option;
                        time_from_prev = time_from_prev_option;
                        min_value = h_c[c];
                        next = c;
                    } else if (time_to_destination_stack_option == time_to_destination_stack && time_from_prev_option < time_from_prev) {
                        time_from_prev = time_from_prev_option;
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

    private static int get_next_highest_due_date_value_Jovanovic(int [][] c_info, int [][] s_info, boolean consider_time, int next, int[] d_c, int [][] f_c_s, Object[] next_options, boolean next_option_found, int tiers, int stacks_per_bay) {
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
                    double time_to_destination_stack_option = (double) Math.abs(c_info[next_option][0]/stacks_per_bay - d_c[next_option]/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next_option][0] % stacks_per_bay - d_c[next_option] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - c_info[next_option][1]) + (tiers - (s_info[d_c[next_option]][0] - f_c_s[next_option][d_c[next_option]]))) * 2 * 15.0;
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

    public static void stack_filling_Jovanovic(int [][] c_info, int [][] s_info, int stacks, int stacks_per_bay, int tiers, String stack_filling, boolean multiple_bays) {
        int next_stack = c_info[next_block][0];
        int next_prio = c_info[next_block][2];
        int height = s_info[c_info[next_block][0]][0];
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
                    relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay, multiple_bays);
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
                        relocate(c_info, s_info, filling_blocks[c][0], filling_blocks[c][1], next_stack, tiers, stacks_per_bay, multiple_bays);
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
                        relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                        current_height++;
                    }
                } else {
                    current_height = tiers;
                }
            }
        }
    }
}
