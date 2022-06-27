import java.util.*;
public class Relocation_Jovanovic {

    static boolean same_stack; //true, falls next in den gleichen Stapel umgelagert werden sol, indem er sich befindet
    static boolean same_stack_over; //true, falls same_stack = true und Blöcke über next liegen
    static boolean same_stack_under; //true, falls same_stack = true und Blöcke unter next liegen
    static boolean next_to_stopover_stack_prevent_deadlock; //true, falls same_stack = true und next zwischengelagert werden muss, um einen deadlock zu verhindern
    static int stopover_stack_prevent_deadlock; //Stapel, in dem next zwischengelagert wird, um einen deadlock zu verhindern
    static boolean deadlock = false; //true wenn deadlock nicht verhindert werden kann
    static int deadlock_count; //Anzahl der deadlocks
    static int [] deadlock_next;
    static int [] order_relocations; //Reihenfolge, in welcher Blöcke, die next blockieren und nicht unter next liegen, umgelagert werden sollen
    static int [] selected_stacks; //Stapel, in welche Blöcke, die next blockieren, umgelagert werden sollen
    static int [] same_stack_below; //Reihenfolge, in welcher Blöcke, die next blockieren und unter next liegen, umgelagert werden sollen (nur im Fall, dass destination stack von next gleich seinem aktuellen stack ist)

    public static void relocate_next_Jovanovic(int [][][] copy, int next_block, int [][] c_info, int [][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays, int stacks, int tiers, int stacks_per_bay) {
        if (next_to_stopover_stack_prevent_deadlock) {
            int prio = c_info[next_block][2];
            int next_stack = stopover_stack_prevent_deadlock;

            Relocation.relocate(c_info, s_info, next_block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            next_to_stopover_stack_prevent_deadlock = false;
        } else if (!same_stack_under) {
            int prio = c_info[next_block][2];
            int next_stack = d_c[next_block];

            Relocation.relocate(c_info, s_info, next_block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
        } else {
            //wenn s gleich ds und noch ein Block unter c umgelagert werden muss bevor c an Zielstelle gelangt, muss c erst in einen anderen Block
            //erst stack für below aussuchen, da c nur zu nächstem Stack sollte
            int [][] s_info_help = new int [stacks][2];
            for (int s = 0; s < stacks; s++) {
                s_info_help[s][0] = s_info[s][0];
                s_info_help[s][1] = s_info[s][1];
            }
            int [] selected_stack = new int [same_stack_below.length];
            for (int c = 0; c < same_stack_below.length; c++) {
                TreeSet<Integer> stack_options = new TreeSet<>();
                switch (stack_selection) {
                    case "The Lowest Position" -> {
                        int minimum_stack_height = tiers;
                        for (int s = 0; s < stacks; s++) {
                            if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                                if (!((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
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
                    }
                    case "Lowest Priority Index" -> {
                        int highest_due_date_value = 0;
                        for (int s = 0; s < stacks; s++) {
                            int highest_due_date_value_option = 0;
                            if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                                if (!((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
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
                                if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                                    if (!((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
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
                    }
                    case "MinMax" -> {
                        int highest_due_date_value = 0;
                        for (int s = 0; s < stacks; s++) {
                            int highest_due_date_value_option = 0;
                            if (s != c_info[same_stack_below[c]][0] && (s_info_help[s][0] + 1) < tiers) {
                                if (!((s_info_help[s][0] + 2) == tiers && stacks == 3)) {
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
                }
                if (stack_options.size() == 0) {
                    deadlock = true;
                } else if (stack_options.size() == 1 || !consider_time) {
                    selected_stack[c] = stack_options.first();
                } else if (consider_time) {
                    selected_stack[c] = Params.compute_nearest_stack(stack_options, tiers, stacks_per_bay, c_info[same_stack_below[c]][0], c_info[same_stack_below[c]][1], 0);
                }
                //da Umlagerung noch nicht direkt durchgeführt wird, muss sichergestellt werden, dass nicht mehr Blöcke als möglich in einen Stack hinein umgelagert werden
                if (stack_options.size() != 0) {
                    s_info_help[selected_stack[c]][0] += 1;
                }
            }
            if (PreMarshalling.print_info) {
                System.out.println("selected_stack: " + Arrays.toString(selected_stack));
            }

            if (!deadlock) {
                //next in nächsten Stack s umlagern, der keinem der Stacks aus selected_stack[] entspricht (am besten der nächste Platz)
                double time_to_stopover_stack = 100000;
                int stopover_stack = 0;
                for (int s = 0; s < stacks; s++) {
                    //prüft, ob s einem der selected_stacks für same_stack_below-Blöcke entspricht
                    boolean selected_stacks_contains_s = false;
                    for (int i : selected_stack) {
                        if (s == i) {
                            selected_stacks_contains_s = true;
                            break;
                        }
                    }
                    //stopover_stack darf weder einem der selected_stacks für same_stack_below-Blöcke, noch dem destination stack entsprechen
                    //außerdem darf stopover_stack nicht voll sein
                    if (!selected_stacks_contains_s && s != d_c[next_block] && s_info[s][0] < tiers - 1) {
                        if (consider_time) {
                            double time_to_stopover_stack_option = Params.get_time(c_info[next_block][0], s, c_info[next_block][1], s_info[s][0], tiers, stacks_per_bay);
                            if (time_to_stopover_stack_option < time_to_stopover_stack) {
                                time_to_stopover_stack = time_to_stopover_stack_option;
                                stopover_stack = s;
                            }
                        } else {
                            stopover_stack = s;
                            s = stacks;
                        }
                    }
                }
                //next in stopover_stack umlagern
                int block = next_block;
                int prio = c_info[block][2];
                int next_stack = stopover_stack;

                Relocation.relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);

                //jeweils Block c aus same_stack_below[c] in selected_stack[c] umlagern
                for (int c = 0; c < same_stack_below.length; c++) {
                    block = same_stack_below[c];
                    prio = c_info[block][2];
                    next_stack = selected_stack[c];

                    Relocation.relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                }

                //next von stopover_stack in ds umlagern
                block = next_block;
                prio = c_info[block][2];
                next_stack = d_c[next_block];

                Relocation.relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            }
        }
    }

    public static void relocate_blocking_blocks_Jovanovic(int[][][] copy, int next_block, int [][] c_info, int [][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays, int stacks, int stacks_per_bay, int tiers) {
        get_selected_stacks_Jovanovic(copy, next_block, c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);

        if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
            //relocation durchführen in order_relocation und Updates
            for (int c = 0; c < order_relocations.length; c++) {
                int block = order_relocations[c];
                int prio = c_info[block][2];
                int next_stack = selected_stacks[c];

                Relocation.relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
            }
        } else if (next_to_stopover_stack_prevent_deadlock && !deadlock) {
            relocate_next_Jovanovic(copy, next_block, c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
            get_selected_stacks_Jovanovic(copy, next_block, c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);
            if (!(next_to_stopover_stack_prevent_deadlock || deadlock)) {
                //relocation durchführen in order_relocation und Updates
                for (int c = 0; c < order_relocations.length; c++) {
                    int block = order_relocations[c];
                    int prio = c_info[block][2];
                    int next_stack = selected_stacks[c];

                    Relocation.relocate(c_info, s_info, block, prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                }
            }
        }
    }
    public static void compute__order_relocations__same_stack_below_Jovanovic(int [][][] copy, int[][] c_info, int[][] s_info, int[] d_c, int [][] g_c_s, int [][] f_c_s, int next) {
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
        } else { //falls der destination stack von next dem source stack entspricht, werden die benötigten Umlagerungen in order_relocations (blockierende Blöcke über next) und same_stack_under (blockierende Blöcke unter next) aufgeteilt, da dazwischen next umgelagert werden muss
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

    public static void get_selected_stacks_Jovanovic(int [][][] copy, int next_block, int[][] c_info, int[][] s_info, int [] d_c, String stack_selection, boolean consider_time, boolean multiple_bays,  int stacks, int stacks_per_bay, int tiers) {
        int [][] s_info_help = new int [stacks][2];
        for (int s = 0; s < stacks; s++) {
            s_info_help[s][0] = s_info[s][0];
            s_info_help[s][1] = s_info[s][1];
        }
        selected_stacks = new int [order_relocations.length];
        stopover_stack_prevent_deadlock = 0;
        for (int c = 0; c < order_relocations.length; c++) {
            TreeSet<Integer> stack_options = new TreeSet<>();
            switch (stack_selection) {
                case "The Lowest Position" -> {
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
                        }
                    }
                }
                case "Lowest Priority Index" -> {
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
                }
                case "Min Max" -> {
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
            }
            if (stack_options.size() == 0) {
                //relocate next to stopover_stack first before continuing with relocations
                if (c_info[next_block][1] != tiers-1) {
                    if (copy[c_info[next_block][0]][c_info[next_block][1] + 1][0] == 0) {
                        next_to_stopover_stack_prevent_deadlock = true;
                        TreeSet<Integer> stopover_stack_options = new TreeSet<>();
                        for (int s = 0; s < stacks; s++) {
                            if (s != d_c[next_block] && s != c_info[next_block][0] && s_info[s][0] != tiers-1) {
                                stopover_stack_options.add(s);
                            }
                        }
                        if (stopover_stack_options.size() == 0) {
                            deadlock = true;
                            c = order_relocations.length;
                        } else if (stopover_stack_options.size() == 1 || !consider_time) {
                            stopover_stack_prevent_deadlock = stopover_stack_options.first();
                        } else if (consider_time) {
                            stopover_stack_prevent_deadlock = Params.compute_nearest_stack(stopover_stack_options, tiers, stacks_per_bay, c_info[next_block][0], c_info[next_block][1], 0, s_info_help);
                        }
                    } else {
                        deadlock = true;
                        c = order_relocations.length;
                    }
                } else {
                    next_to_stopover_stack_prevent_deadlock = true;
                    TreeSet<Integer> stopover_stack_options = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s != d_c[next_block] && s != c_info[next_block][0] && s_info[s][0] != tiers-1) {
                            stopover_stack_options.add(s);
                        }
                    }
                    if (stopover_stack_options.size() == 0) {
                        //in vollem Stack einen PLatz freiräumen, um next zwischenzulagern
                        for (int s = 0; s < stacks; s++) {
                            if (s_info[s][0] == tiers-1) {
                                stopover_stack_options.add(s);
                            }
                        }
                        if(!consider_time) {
                            stopover_stack_prevent_deadlock = stopover_stack_options.first();
                        } else {
                            stopover_stack_prevent_deadlock = Params.compute_nearest_stack(stopover_stack_options, tiers, stacks_per_bay, c_info[next_block][0], c_info[next_block][1], 0, s_info_help);
                        }
                    } else if (stopover_stack_options.size() == 1 || !consider_time) {
                        stopover_stack_prevent_deadlock = stopover_stack_options.first();
                    } else if (consider_time) {
                        stopover_stack_prevent_deadlock = Params.compute_nearest_stack(stopover_stack_options, tiers, stacks_per_bay, c_info[next_block][0], c_info[next_block][1], 0, s_info_help);
                    }
                    if (stopover_stack_options.size() != 0) {
                        relocate_next_Jovanovic(copy, next_block, c_info, s_info, d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
                    }
                }

            } else if (stack_options.size() == 1 || !consider_time) {
                selected_stacks[c] = stack_options.first();
                s_info_help[selected_stacks[c]][0] += 1;
                deadlock = false;
            } else if (consider_time) {
                selected_stacks[c] = Params.compute_nearest_stack(stack_options, tiers, stacks_per_bay, c_info[order_relocations[c]][0], c_info[order_relocations[c]][1], 0, s_info_help);
                s_info_help[selected_stacks[c]][0] += 1;
                deadlock = false;
            }
        }
    }

    public static void get_next_Jovanovic(int [][][] copy, int next_block, int prev_block, int [][] c_info, int [][] s_info, int [] d_c, int [][] w_c_s, int [][] f_c_s, String next_selection, boolean consider_time, int containers, int tiers, int stacks_per_bay) {
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
            switch (next_selection) {
                case "function h_c" ->
                        Relocation.next_block = get_next_function_h_c_Jovanovic(copy, prev_block, c_info, s_info, consider_time, next_block, d_c, w_c_s, f_c_s, next_options, next_option_found, tiers, stacks_per_bay, containers);
                case "highest due date value" ->
                        Relocation.next_block = get_next_highest_due_date_value_Jovanovic(prev_block, c_info, s_info, consider_time, next_block, d_c, f_c_s, next_options, next_option_found, tiers, stacks_per_bay);
            }
        } else {
            System.out.println("DEADLOCK! No next_option found.");
            PreMarshalling.solution_found = false;
        }
    }

    public static int get_next_function_h_c_Jovanovic(int [][][] copy, int prev_block, int [][] c_info, int [][] s_info, boolean consider_time, int next, int[] d_c, int[][] w_c_s, int[][] f_c_s, Object[] next_options, boolean next_option_found, int tiers, int stacks_per_bay, int containers) {
        //number of forced relocations: in this approximation, a forced relocation occurs only if a block c is being relocated and all the top stack due date values are larger than p(c)
        //if all blocks in a stack are well located we shall consider the stack having due date value zero
        int[] fr_c_s = new int[containers];
        //werden im Paper nur forced relocations aus stack s oder auch aus destination stack ss betrachtet? → hier werden beide betrachtet
        fr_c_s = Params_Jovanovic.compute_fr_c_s(copy, Params.bay_info, Params.c_info, Params.s_info, fr_c_s, d_c, next_options);

        int[] h_c = new int[containers];
        for (Object nextOption : next_options) {
            int c = (int) nextOption;
            h_c[c] = w_c_s[c][d_c[c]] + fr_c_s[c] - c_info[c][2];
        }

        //indice of next block to be relocated is calculated
        int min_value = 1000000;
        double time_to_destination_stack = 1000000;
        //double time_from_prev = 1000000;
        if (next_option_found) {
            for (Object nextOption : next_options) {
                int c = (int) nextOption;
                double time_from_prev = Params.get_time(c_info[next][0], c_info[prev_block][0], s_info[c_info[next][0]][0], c_info[prev_block][1], tiers, stacks_per_bay);
                if (h_c[c] < min_value) {
                    next = c;
                    min_value = h_c[c];
                    if (s_info[d_c[next]][0] != -1) {
                        time_to_destination_stack = time_from_prev + Params.get_time(c_info[next][0], d_c[next], c_info[next][1], s_info[d_c[next]][0] - f_c_s[next][d_c[next]] + 1, tiers, stacks_per_bay);
                    } else {
                        time_to_destination_stack = time_from_prev + Params.get_time(c_info[next][0], d_c[next], c_info[next][1], 0, tiers, stacks_per_bay);
                    }
                } else if (consider_time && h_c[c] == min_value) {
                    double time_to_destination_stack_option;
                    if (s_info[d_c[c]][0] != -1) {
                        time_to_destination_stack_option = time_from_prev + Params.get_time(c_info[c][0], d_c[c], c_info[c][1], s_info[d_c[c]][0] - f_c_s[c][d_c[c]] + 1, tiers, stacks_per_bay);
                    } else {
                        time_to_destination_stack_option = time_from_prev + Params.get_time(c_info[c][0], d_c[c], c_info[c][1], 0, tiers, stacks_per_bay);
                    }
                    if (time_to_destination_stack_option < time_to_destination_stack) {
                        time_to_destination_stack = time_to_destination_stack_option;
                        min_value = h_c[c];
                        next = c;
                    } else if (time_to_destination_stack_option == time_to_destination_stack) {
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

    private static int get_next_highest_due_date_value_Jovanovic(int prev_block, int [][] c_info, int [][] s_info, boolean consider_time, int next, int[] d_c, int [][] f_c_s, Object[] next_options, boolean next_option_found, int tiers, int stacks_per_bay) {
        int highest_due_date = 0;
        double time_to_destination_stack = 1000000;
        if (next_option_found) {
            for (Object next_option : next_options) {
                int c = (int) next_option;
                double time_from_prev = (double) Math.abs(c_info[next][0] / stacks_per_bay - c_info[prev_block][0] / stacks_per_bay) * 2 * 1.875 + (double) Math.abs(c_info[next][0] % stacks_per_bay - c_info[prev_block][0] % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - s_info[c_info[next][0]][0]) + (tiers - c_info[prev_block][1])) * 2 * 15.0;
                if (c_info[c][2] > highest_due_date) {
                    time_to_destination_stack = time_from_prev + Params.get_time(c_info[c][0], d_c[c], c_info[c][1], s_info[d_c[c]][0] - f_c_s[c][d_c[c]], tiers, stacks_per_bay);
                    highest_due_date = c_info[c][2];
                    next = c;
                } else if (consider_time && c_info[c][2] == highest_due_date) {
                    double time_to_destination_stack_option = time_from_prev + Params.get_time(c_info[c][0], d_c[c], c_info[c][1], s_info[d_c[c]][0] - f_c_s[c][d_c[c]], tiers, stacks_per_bay);
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

    public static void stack_filling_Jovanovic(int [][][] copy, int next_block, int prev_block, int [][] c_info, int [][] s_info, int stacks, int stacks_per_bay, int tiers, String stack_filling, boolean multiple_bays, boolean consider_time) {
        int next_stack = c_info[next_block][0];
        int next_prio = c_info[next_block][2];
        int height = s_info[c_info[next_block][0]][0];
        int current_height = height;
        int candidate_block;
        int candidate_stack;
        int candidate_prio;
        boolean candidate_found;
        switch (stack_filling) {
            case "Standard" -> {
                while (current_height < tiers - 1) {
                    candidate_found = false;
                    candidate_block = 0;
                    candidate_prio = 0;
                    TreeSet<Integer> block_options = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s != next_stack && s_info[s][0] != -1) {
                            if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] < next_prio) {
                                int candidate_prio_option = copy[s][s_info[s][0]][1];
                                if (candidate_prio_option > candidate_prio) {
                                    candidate_block = copy[s][s_info[s][0]][0] - 1;
                                    block_options.clear();
                                    block_options.add(candidate_block);
                                    candidate_found = true;
                                    candidate_prio = candidate_prio_option;
                                }
                            } else if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] == next_prio) {
                                int candidate_prio_option = copy[s][s_info[s][0]][1];
                                if (candidate_prio_option > candidate_prio) {
                                    candidate_block = copy[s][s_info[s][0]][0] - 1;
                                    block_options.add(candidate_block);
                                    candidate_prio = candidate_prio_option;
                                }
                            }
                        }
                    }
                    if (block_options.size() == 1 || !consider_time) {
                        candidate_block = block_options.first();
                    } else if (block_options.size() > 1 && consider_time) {
                        candidate_block = Params.compute_nearest_stack(block_options, tiers, stacks_per_bay, c_info[prev_block][0], c_info[prev_block][1], 1);
                        candidate_prio = c_info[candidate_block][2];
                    }
                    if (candidate_found) {
                        if (PreMarshalling.print_info) {
                            System.out.println("Stack filling!");
                        }
                        Relocation.relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                        current_height++;
                    } else {
                        current_height = tiers;
                    }
                }
            }
            case "Safe" -> {
                int[][] filling_blocks = new int[tiers - 1][2]; //block, prio
                int filling_count = 0;
                int[][] s_info_help = new int[stacks][2];
                for (int s = 0; s < stacks; s++) {
                    s_info_help[s][0] = s_info[s][0];
                    s_info_help[s][1] = s_info[s][1];
                }
                while (current_height < tiers - 1) {
                    candidate_found = false;
                    candidate_block = 0;
                    candidate_stack = 0;
                    candidate_prio = 0;
                    TreeSet<Integer> block_options = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s != next_stack && s_info_help[s][0] != -1) {
                            if (s_info_help[s][1] == 0 && copy[s][s_info_help[s][0]][1] < next_prio) {
                                int candidate_prio_option = copy[s][s_info_help[s][0]][1];
                                if (candidate_prio_option > candidate_prio) {
                                    candidate_block = copy[s][s_info_help[s][0]][0] - 1;
                                    block_options.clear();
                                    block_options.add(candidate_block);
                                    candidate_stack = s;
                                    candidate_found = true;
                                    candidate_prio = candidate_prio_option;
                                } else if (candidate_prio_option == candidate_prio) {
                                    candidate_block = copy[s][s_info_help[s][0]][0] - 1;
                                    block_options.add(candidate_block);
                                    candidate_stack = s;
                                }
                            }
                        }
                    }
                    if (block_options.size() == 1 || !consider_time) {
                        candidate_block = block_options.first();
                    } else if (block_options.size() > 1 && consider_time) {
                        candidate_block = Params.compute_nearest_stack(block_options, tiers, stacks_per_bay, c_info[prev_block][0], c_info[prev_block][1], 1);
                        candidate_prio = c_info[candidate_block][2];
                        candidate_stack = c_info[candidate_block][0];
                    }
                    if (candidate_found) {
                        filling_blocks[filling_count][0] = candidate_block;
                        filling_blocks[filling_count][1] = candidate_prio;
                        s_info_help[candidate_stack][0] -= 1;
                        s_info_help[candidate_stack][1] = copy[candidate_stack][c_info[candidate_block][1] - 1][2];
                        current_height++;
                        filling_count++;
                    } else {
                        current_height = tiers;
                    }
                }
                int alpha = 1;
                if ((tiers - (height + 1 + filling_count)) <= alpha) {
                    for (int[] filling_block : filling_blocks) {
                        if (filling_block[1] != 0) {
                            if (PreMarshalling.print_info) {
                                System.out.println("Stack filling!");
                            }
                            Relocation.relocate(c_info, s_info, filling_block[0], filling_block[1], next_stack, tiers, stacks_per_bay, multiple_bays);
                        }
                    }
                }
            }
            case "Stop" -> {
                while (current_height < tiers - 1) {
                    candidate_found = false;
                    candidate_block = 0;
                    candidate_prio = 0;
                    TreeSet<Integer> block_options = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s != next_stack && s_info[s][0] != -1) {
                            if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] < next_prio) {
                                int candidate_prio_option = copy[s][s_info[s][0]][1];
                                if (candidate_prio_option > candidate_prio) {
                                    candidate_block = copy[s][s_info[s][0]][0] - 1;
                                    block_options.clear();
                                    block_options.add(candidate_block);
                                    candidate_found = true;
                                    candidate_prio = candidate_prio_option;
                                } else if (candidate_prio_option == candidate_prio) {
                                    candidate_block = copy[s][s_info[s][0]][0] - 1;
                                    block_options.add(candidate_block);
                                }
                            }
                        }
                    }
                    if (block_options.size() == 1 || !consider_time) {
                        candidate_block = block_options.first();
                    } else if (block_options.size() > 1 && consider_time) {
                        candidate_block = Params.compute_nearest_stack(block_options, tiers, stacks_per_bay, c_info[prev_block][0], c_info[prev_block][1], 1);
                        candidate_prio = c_info[candidate_block][2];
                    }
                    if (candidate_found) {
                        //wenn unter ausgewähltem Block ein Block
                        if (s_info[c_info[candidate_block][0]][0] > 0 && candidate_prio < copy[c_info[candidate_block][0]][c_info[candidate_block][1] - 1][2]) {
                            current_height = tiers;
                        } else {
                            if (PreMarshalling.print_info) {
                                System.out.println("Stack filling!");
                            }
                            Relocation.relocate(c_info, s_info, candidate_block, candidate_prio, next_stack, tiers, stacks_per_bay, multiple_bays);
                            current_height++;
                        }
                    } else {
                        current_height = tiers;
                    }
                }
            }
        }
    }
}
