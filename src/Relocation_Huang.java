import java.util.*;
public class Relocation_Huang {

    static double beta;
    static double beta_h; //adjusting coefficient with values between 0 and 1
    //complete
    static int next_stack_R;
    static int next_stack_W;
    //deconstruct
    static int next_stack_R_low;
    static int next_stack_R_deconstruct;
    static int next_stack_W_deconstruct;
    static int [] next_stack_R_low_checked;
    static boolean deconstruct_low_R_stack = false;
    //create empty_stack
    static int empty_stack;
    static int step_empty_stack;

    public static void complete_high_R_stacks(int [][][] copy, int [][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        beta_h = Math.ceil(beta * tiers);
        boolean next_stack_R_found = true;
        int [] next_stack_R_checked = new int [stacks];

        while (next_stack_R_found) {
            //compute receiving high R stack
            int height = 0;
            next_stack_R_found = false;
            TreeSet<Integer> stack_options_R = new TreeSet<>();
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] >= beta_h && s_info[s][0] != tiers - 1 && next_stack_R_checked[s] == 0 ) {
                    if (s_info[s][0] > height) {
                        next_stack_R_found = true;
                        stack_options_R.clear();
                        stack_options_R.add(s);
                        height = s_info[s][0];
                    } else if (s_info[s][0] == height) {
                        stack_options_R.add(s);
                    }
                }
            }
            if (stack_options_R.size() != 0 && (stack_options_R.size() == 1 || !consider_time)) {
                next_stack_R = stack_options_R.first();
            } else if (stack_options_R.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
                next_stack_R = Params.compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
            }
            if (next_stack_R_found) {
                boolean next_stack_W_found = true;
                while (next_stack_W_found && s_info[next_stack_R][0] != tiers - 1) {
                    next_stack_W_found = false;
                    //compute giving W stack
                    int next_stack_R_prio = copy[next_stack_R][s_info[next_stack_R][0]][1];
                    int prio = 0;
                    TreeSet<Integer> stack_options_W = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s_info[s][1] == 0) {
                            if (copy[s][s_info[s][0]][1] <= next_stack_R_prio && copy[s][s_info[s][0]][1] > prio) {
                                next_stack_W_found = true;
                                prio = copy[s][s_info[s][0]][1];
                                stack_options_W.clear();
                                stack_options_W.add(s);
                            } else if (copy[s][s_info[s][0]][1] <= next_stack_R_prio && copy[s][s_info[s][0]][1] == prio) {
                                stack_options_W.add(s);
                            }
                        }
                    }
                    if (stack_options_W.size() == 0) {
                        next_stack_R_checked[next_stack_R] = 1;
                    } else if (stack_options_W.size() == 1 || !consider_time) {
                        next_stack_W = stack_options_W.first();
                    } else if (consider_time) { // falls mehrere giving stacks möglich, denjenigen mit der geringsten Entfernung zu receiving stack wählen
                        next_stack_W = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R, s_info[next_stack_R][0], 0);
                    }
                    if (next_stack_R_found && next_stack_W_found) {
                        if (PreMarshalling.print_info) {
                            System.out.println("Complete high R stack!");
                        }
                        int candidate_block = copy[next_stack_W][s_info[next_stack_W][0]][0] - 1;
                        int next_stack_W_prio = copy[next_stack_W][s_info[next_stack_W][0]][1];
                        Relocation.relocate(c_info, s_info, candidate_block, next_stack_W_prio, next_stack_R, tiers, stacks_per_bay, multiple_bays);
                    }
                }
            }
        }
    }

    public static void complete_low_R_stacks(int [][][] copy, int [][][] current_bay, int [][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        beta_h = beta * tiers;
        boolean next_stack_R_found = true;
        boolean next_stack_W_found = true;
        int [] next_stack_R_checked = new int [stacks];

        while (next_stack_R_found) {
            //compute receiving low R stack
            next_stack_R_found = false;
            TreeSet<Integer> stack_options_R = new TreeSet<>();
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_checked[s] == 0 ) {
                    next_stack_R_found = true;
                    stack_options_R.add(s);
                }
            }
            if (stack_options_R.size() != 0 && (stack_options_R.size() == 1 || !consider_time)) {
                next_stack_R = stack_options_R.first();
            } else if (stack_options_R.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
                next_stack_R = Params.compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
            }
            if (next_stack_R_found) {
                next_stack_W_found = true;
                while (next_stack_W_found && s_info[next_stack_R][0] != tiers - 1) {
                    next_stack_W_found = false;
                    //compute giving W stack
                    int next_stack_R_prio = copy[next_stack_R][s_info[next_stack_R][0]][1];
                    int prio = 0;
                    TreeSet<Integer> stack_options_W = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        //wenn innerhalb von deconstruct_low_R_stack() complete_low_R_stack() aufgerufen wird, darf nicht der Block, der gerade umgelagert wurde zurück umgelagert werden (sonst loop)
                        if (s_info[s][1] == 0 && !(deconstruct_low_R_stack && s == c_info[Relocation.prev_block][0])) {
                            if (copy[s][s_info[s][0]][1] <= next_stack_R_prio && copy[s][s_info[s][0]][1] > prio) {
                                next_stack_W_found = true;
                                prio = copy[s][s_info[s][0]][1];
                                stack_options_W.clear();
                                stack_options_W.add(s);
                            } else if (copy[s][s_info[s][0]][1] <= next_stack_R_prio && copy[s][s_info[s][0]][1] == prio) {
                                stack_options_W.add(s);
                            }
                        }
                    }
                    if (stack_options_W.size() == 0) {
                        next_stack_R_checked[next_stack_R] = 1;
                    } else if (stack_options_W.size() == 1 || !consider_time) {
                        next_stack_W = stack_options_W.first();
                    } else if (consider_time) { // falls mehrere giving stacks möglich, denjenigen mit der geringsten Entfernung zu receiving stack wählen
                        next_stack_W = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R, s_info[next_stack_R][0], 0);
                    }
                    if (next_stack_R_found && next_stack_W_found) {
                        if (PreMarshalling.print_info) {
                            System.out.println("Complete low R stack!");
                        }
                        int candidate_block = copy[next_stack_W][s_info[next_stack_W][0]][0] - 1;
                        int next_stack_W_prio = copy[next_stack_W][s_info[next_stack_W][0]][1];
                        Relocation.relocate(c_info, s_info, candidate_block, next_stack_W_prio, next_stack_R, tiers, stacks_per_bay, multiple_bays);
                    }
                }
            }
            if (!next_stack_R_found || !next_stack_W_found) {
                if (!next_stack_R_found) {
                    if (PreMarshalling.print_info) {
                        System.out.println("No next_stack_R could be found!");
                    }
                } else if (s_info[next_stack_R][0] < beta_h) {
                    if (PreMarshalling.print_info) {
                        System.out.println("Next_stack_R " + next_stack_R + " doesn't reach or exceed beta_h " + beta_h + ". Possible relocations canceled!");
                    }
                    next_stack_R_checked[next_stack_R] = 1;
                    copy = BayInstance.copy_bay(current_bay, Relocation.copy, stacks, tiers);
                    Params.compute__c_info__s_info(copy, stacks, tiers);
                    //Wenn die relocations rückgängig gemacht werden, müssen sie auch aus relocations_on_hold gelöscht werden
                    PreMarshalling.relocations_on_hold.clear();
                } else {
                    if (PreMarshalling.print_info) {
                        System.out.println("Next_stack_R " + next_stack_R + " does reach or exceed beta_h " + beta_h + ".");
                    }
                    next_stack_R_checked[next_stack_R] = 1;
                    PreMarshalling.current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
                    current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
                    PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);
                }
            }
        }
    }

    public static void deconstruct_low_R_stacks(int [][][] copy, int [][][] current_bay, int [][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        beta_h = beta * tiers;

        boolean next_stack_R_low_found = false;
        //verhindern, dass deconstructing bei allen low_R stacks nicht erfolgreich ist, sowie keine Umlagerungen stattfinden und im nächsten Durchgang erneut durchgeführt wird (loop)
        //Abfrage bereits hier notwendig, da sonst next_stack_R_low_checked reinitialisiert wird
        if (PreMarshalling.step == 1) {
            next_stack_R_low_found = true;
        } else {
            if (PreMarshalling.relocation_count_current == Relocation.relocations_count) {
                for (int s = 0; s < stacks; s++) {
                    if (s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_low_checked[s] == 0 && s != c_info[Relocation.prev_block][0]) {
                        next_stack_R_low_found = true;
                        s = stacks;
                    }
                }
            } else {
                next_stack_R_low_found = true;
            }
        }
        next_stack_R_low_checked = new int [stacks];

        while (next_stack_R_low_found && !Params.sorted) {
            //compute giving low R stack
            next_stack_R_low_found = false;
            TreeSet<Integer> stack_options_R_low = new TreeSet<>();
            int next_stack_R_low_prio = 100000;
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_low_checked[s] == 0 && s != c_info[Relocation.prev_block][0] && copy[s][s_info[s][0]][1] < next_stack_R_low_prio) {
                    next_stack_R_low_found = true;
                    stack_options_R_low.clear();
                    stack_options_R_low.add(s);
                    next_stack_R_low_prio = copy[s][s_info[s][0]][1];
                } else if(s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_low_checked[s] == 0 && s != c_info[Relocation.prev_block][0] && copy[s][s_info[s][0]][1] == next_stack_R_low_prio) {
                    stack_options_R_low.add(s);
                }
            }
            if (stack_options_R_low.size() != 0 && (stack_options_R_low.size() == 1 || !consider_time)) {
                next_stack_R_low = stack_options_R_low.first();
            } else if (stack_options_R_low.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
                next_stack_R_low = Params.compute_nearest_stack(stack_options_R_low, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
            }
            if (next_stack_R_low_found && s_info[next_stack_R_low][0] != -1) {
                boolean next_stack_R_found = true;
                while (next_stack_R_found && !Params.sorted) {
                    next_stack_R_found = false;
                    //compute receiving R stack
                    next_stack_R_low_prio = copy[next_stack_R_low][s_info[next_stack_R_low][0]][1];
                    int prio = 0;
                    TreeSet<Integer> stack_options_R = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s_info[s][1] == 1 && s_info[s][0] != -1 && s != next_stack_R_low) {
                            if (copy[s][s_info[s][0]][1] >= next_stack_R_low_prio && copy[s][s_info[s][0]][1] > prio && s_info[s][0] != tiers - 1) {
                                next_stack_R_found = true;
                                prio = copy[s][s_info[s][0]][1];
                                stack_options_R.clear();
                                stack_options_R.add(s);
                            } else if (copy[s][s_info[s][0]][1] <= next_stack_R_low_prio && copy[s][s_info[s][0]][1] == prio && s_info[s][0] != tiers - 1) {
                                stack_options_R.add(s);
                            }
                        }
                    }
                    if (stack_options_R.size() == 0) {
                        next_stack_R_low_checked[next_stack_R_low] = 1;
                    } else if (stack_options_R.size() == 1 || !consider_time) {
                        next_stack_R_deconstruct = stack_options_R.first();
                    } else if (consider_time) { // falls mehrere giving stacks möglich, denjenigen mit der geringsten Entfernung zu receiving stack wählen
                        next_stack_R_deconstruct = Params.compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, next_stack_R_low, s_info[next_stack_R_low][0], 0);
                    }
                    if (next_stack_R_low_found && next_stack_R_found) {
                        if (PreMarshalling.print_info) {
                            System.out.println("Deconstruct low R stack! Move from R_low to R.");
                        }
                        deconstruct_low_R_stack = true;
                        int candidate_block = copy[next_stack_R_low][s_info[next_stack_R_low][0]][0] - 1;
                        Relocation.relocate(c_info, s_info, candidate_block, next_stack_R_low_prio, next_stack_R_deconstruct, tiers, stacks_per_bay, multiple_bays);
                        current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
                        PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);

                        complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
                        PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);

                        if (s_info[next_stack_R_low][0] == -1 || s_info[next_stack_R_low][0] >= beta_h) {
                            if (s_info[next_stack_R_low][0] == -1) {
                                move_W_to_empty_stack(copy, c_info, s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
                            }
                            Params.check_sorted(stacks);
                            next_stack_R_found = false;
                            //next_stack_R_low_found = false;
                            next_stack_R_low_checked[next_stack_R_low] = 1;
                        }
                    } else if (next_stack_R_low_found) {
                        boolean next_stack_W_found = true;
                        while (next_stack_W_found && !Params.sorted) {
                            next_stack_W_found = false;
                            prio = 0;
                            TreeSet<Integer> stack_options_W = new TreeSet<>();
                            for (int s = 0; s < stacks; s++) {
                                if (s_info[s][1] == 0) {
                                    if (copy[s][s_info[s][0]][1] <= next_stack_R_low_prio && copy[s][s_info[s][0]][1] > prio && s_info[s][0] != tiers - 1) {
                                        next_stack_W_found = true;
                                        prio = copy[s][s_info[s][0]][1];
                                        stack_options_W.clear();
                                        stack_options_W.add(s);
                                    } else if (copy[s][s_info[s][0]][1] <= next_stack_R_low_prio && copy[s][s_info[s][0]][1] == prio && s_info[s][0] != tiers - 1) {
                                        stack_options_W.add(s);
                                    }
                                }
                            }
                            if (stack_options_W.size() == 0) {
                                next_stack_W_found = false;
                                prio = 100000;
                                stack_options_W = new TreeSet<>();
                                for (int s = 0; s < stacks; s++) {
                                    if (s_info[s][1] == 0) {
                                        if (copy[s][s_info[s][0]][1] > next_stack_R_low_prio && copy[s][s_info[s][0]][1] < prio && s_info[s][0] != tiers - 1) {
                                            next_stack_W_found = true;
                                            prio = copy[s][s_info[s][0]][1];
                                            stack_options_W.clear();
                                            stack_options_W.add(s);
                                        } else if (copy[s][s_info[s][0]][1] <= next_stack_R_low_prio && copy[s][s_info[s][0]][1] == prio && s_info[s][0] != tiers - 1) {
                                            stack_options_W.add(s);
                                        }
                                    }
                                }
                            }
                            if (stack_options_W.size() == 0) {
                                next_stack_R_low_checked[next_stack_R_low] = 1;
                            } else if (stack_options_W.size() == 1 || !consider_time) {
                                next_stack_W_deconstruct = stack_options_W.first();
                            } else if (consider_time) { // falls mehrere giving stacks möglich, denjenigen mit der geringsten Entfernung zu receiving stack wählen
                                next_stack_W_deconstruct = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R_low, s_info[next_stack_R_low][0], 0);
                            }
                            if (next_stack_R_low_found && next_stack_W_found) {
                                if (PreMarshalling.print_info) {
                                    System.out.println("Deconstruct low R stack! Move from R_low to W.");
                                }
                                deconstruct_low_R_stack = true;
                                int candidate_block = copy[next_stack_R_low][s_info[next_stack_R_low][0]][0] - 1;
                                Relocation.relocate(c_info, s_info, candidate_block, next_stack_R_low_prio, next_stack_W_deconstruct, tiers, stacks_per_bay, multiple_bays);
                                current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
                                PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);

                                complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
                                PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);

                                if (s_info[next_stack_R_low][0] == -1 || s_info[next_stack_R_low][0] >= beta_h) {
                                    if (s_info[next_stack_R_low][0] == -1) {
                                        move_W_to_empty_stack(copy, c_info, s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
                                    }
                                    Params.check_sorted(stacks);
                                    next_stack_W_found = false;
                                    //next_stack_R_low_found = false;
                                    next_stack_R_low_checked[next_stack_R_low] = 1;
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public static void move_W_to_empty_stack(int [][][] copy, int [][] c_info, int [][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        boolean empty_stack_found = false;
        int empty_stack = 0;
        TreeSet<Integer> stack_options_empty = new TreeSet<>();
        for (int s = 0; s < stacks; s++) {
            if (s_info[s][0] == -1 && ! (s == empty_stack && PreMarshalling.step == step_empty_stack + 1)) {
                empty_stack_found = true;
                stack_options_empty.add(s);
            }
        }
        //consider_time to prev_block
        if (stack_options_empty.size() != 0 && (stack_options_empty.size() == 1 || !consider_time)) {
            empty_stack = stack_options_empty.first();
        } else if (stack_options_empty.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
            empty_stack = Params.compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
        }
        if (empty_stack_found) {
            boolean block_in_W_found = true;
            while (block_in_W_found && s_info[empty_stack][0] != tiers-1) {
                int W_block;
                int biggest_prio_among_W = 0;
                int W_stack_height = 0;
                block_in_W_found = false;
                TreeSet<Integer> W_options = new TreeSet<>();
                for (int s = 0; s < stacks; s++) {
                    if (s != c_info[Relocation.prev_block][0] && s != empty_stack && s_info[s][1] == 0 && s_info[empty_stack][0] == -1) {
                        block_in_W_found = true;
                        if (copy[s][s_info[s][0]][1] > biggest_prio_among_W) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.clear();
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        } else if (copy[s][s_info[s][0]][1] == biggest_prio_among_W && W_stack_height < s_info[s][0]) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.clear();
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        } else if (copy[s][s_info[s][0]][1] == biggest_prio_among_W && W_stack_height == s_info[s][0]) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        }
                    }
                    else if (s != c_info[Relocation.prev_block][0] && s != empty_stack &&  s_info[s][1] == 0 && copy[s][s_info[s][0]][1] <= copy[empty_stack][s_info[empty_stack][0]][1]) {
                        block_in_W_found = true;
                        if (copy[s][s_info[s][0]][1] > biggest_prio_among_W) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.clear();
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        } else if (copy[s][s_info[s][0]][1] == biggest_prio_among_W && W_stack_height < s_info[s][0]) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.clear();
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        } else if (copy[s][s_info[s][0]][1] == biggest_prio_among_W && W_stack_height == s_info[s][0]) {
                            W_block = copy[s][s_info[s][0]][0] - 1;
                            W_stack_height = s_info[s][0];
                            W_options.add(W_block);
                            biggest_prio_among_W = copy[s][s_info[s][0]][1];
                        }
                    }
                }
                if (block_in_W_found && !consider_time) {
                    if (PreMarshalling.print_info) {
                        System.out.println("Move containers in W to empty stack!");
                    }
                    W_block = W_options.first();
                    int W_block_prio = c_info[W_block][2];
                    Relocation.relocate(c_info, s_info, W_block, W_block_prio, empty_stack, tiers, stacks_per_bay, multiple_bays);
                } else if (block_in_W_found && consider_time) {
                    if (PreMarshalling.print_info) {
                        System.out.println("Move containers in W to empty stack!");
                    }
                    W_block = Params.compute_nearest_stack(W_options, tiers, stacks_per_bay, empty_stack, s_info[empty_stack][0], 1);
                    int W_block_prio = c_info[W_block][2];
                    Relocation.relocate(c_info, s_info, W_block, W_block_prio, empty_stack, tiers, stacks_per_bay, multiple_bays);
                }
            }
        } else if (!empty_stack_found && PreMarshalling.relocation_count_current == Relocation.relocations_count && step_empty_stack + 1 == PreMarshalling.step) {
            if (beta < 1) {
                beta += 0.1;
                if (PreMarshalling.print_info) {
                    System.out.println("Beta increased by 0.1");
                }
            } else {
                if (PreMarshalling.print_info) {
                    System.out.println("No solution found!");
                }
                PreMarshalling.solution_found = false;
            }
        }
    }

    public static void create_empty_stack(int[][][] copy, int[][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        //wenn kein einzelner empty stack erzeugt werden kann, weil keine destination stacks mit prio ≤ prio von Block aus source vorhanden, dann Block aus source trotzdem umlagern
        boolean stack_source_found = true;
        boolean stack_destination_found = true;
        boolean all_W_stacks_exhausted = false;
        int [] stacks_checked = new int[stacks];
        int W_stack_source = 0;
        while (stack_source_found) {
            stack_source_found = false;
            int source_height = 100000;
            TreeSet<Integer> stack_options_empty = new TreeSet<>();
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][1] == 0 && s_info[s][0] < source_height && stacks_checked[s] != 1) {
                    stack_source_found = true;
                    stack_options_empty.clear();
                    stack_options_empty.add(s);
                    source_height = s_info[s][0];
                } else if (s_info[s][1] == 0 && s_info[s][0] == source_height && stacks_checked[s] != 1) {
                    stack_options_empty.add(s);
                }
            }
            //consider_time to prev_block
            if (stack_options_empty.size() != 0 && (stack_options_empty.size() == 1 || !consider_time)) {
                W_stack_source = stack_options_empty.first();
            } else if (stack_options_empty.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
                W_stack_source = Params.compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
            }
            stack_destination_found = true;
            while (stack_source_found && stack_destination_found && s_info[W_stack_source][0] != -1) {
                //denjenigen W stack mit der niedrigsten Prio des obersten Blockes suchen, da so möglichst keine Blöcke, die bald umgelagert werden sollen, blockiert werden
                //reicht, wenn Prio nicht kleiner ist als Block aus empty Stack → bei consider_time nächsten Stack mit Block mit kleiner/gleichen Prio
                stack_destination_found = false;
                int W_stack_destination = 0;
                int destination_prio = 100000;
                TreeSet<Integer> stack_options_W = new TreeSet<>();
                for (int s = 0; s < stacks; s++) {
                    if (s_info[s][0] != tiers-1 && s != W_stack_source && s_info[s][1] == 0 && (copy[s][s_info[s][0]][1] < destination_prio && !(copy[W_stack_source][s_info[W_stack_source][0]][1] >= copy[s][s_info[s][0]][1]))) {
                        stack_destination_found = true;
                        stack_options_W.clear();
                        stack_options_W.add(s);
                        destination_prio = copy[s][s_info[s][0]][1];
                    } else if (s_info[s][0] != tiers - 1 && s != W_stack_source && s_info[s][1] == 0 && (copy[s][s_info[s][0]][1] == destination_prio || copy[W_stack_source][s_info[W_stack_source][0]][1] >= copy[s][s_info[s][0]][1])) {
                        stack_destination_found = true;
                        stack_options_W.add(s);
                    }
                }
                if (stack_options_W.size() == 0) {
                    stacks_checked[W_stack_source] = 1;
                } else if (stack_options_W.size() == 1 || !consider_time) {
                    W_stack_destination = stack_options_W.first();
                } else if (consider_time) {
                    W_stack_destination = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
                }
                if (stack_destination_found) {
                    int block = copy[W_stack_source][s_info[W_stack_source][0]][0] - 1;
                    int prio = c_info[block][2];
                    if (PreMarshalling.print_info) {
                        System.out.println("Create empty stack: Move containers in W to W!");
                    }
                    Relocation.relocate(c_info, s_info, block, prio, W_stack_destination, tiers, stacks_per_bay, multiple_bays);
                    if (s_info[W_stack_source][0] == -1) {
                        if (PreMarshalling.print_info) {
                            System.out.println("Create empty stack: Stack emptied!");
                        }
                        empty_stack = W_stack_source;
                        step_empty_stack = PreMarshalling.step;
                        stack_source_found = false;
                    }
                } else {
                    all_W_stacks_exhausted = true;
                    for (int s = 0; s < stacks; s++) {
                        if (s != W_stack_source && s_info[s][1] == 0 && s_info[s][0] != tiers-1) {
                            all_W_stacks_exhausted = false;
                            s = stacks;
                        }
                    }
                    if (s_info[W_stack_source][1] == 1 || all_W_stacks_exhausted) {
                        stack_source_found = false;
                    }
                    stacks_checked[W_stack_source] = 1;
                    PreMarshalling.current_bay = BayInstance.copy_bay(Relocation.copy, PreMarshalling.current_bay, stacks, tiers);
                }
            }
        }
        if (PreMarshalling.relocation_count_current == Relocation.relocations_count || all_W_stacks_exhausted) {
            stack_source_found = true;
            stacks_checked = new int[stacks];
            while (stack_source_found) {
                if (!all_W_stacks_exhausted || !stack_destination_found) {
                    stack_source_found = false;
                    W_stack_source = 0;
                    int source_height = 100000;
                    TreeSet<Integer> stack_options_empty = new TreeSet<>();
                    for (int s = 0; s < stacks; s++) {
                        if (s_info[s][1] == 0 && s_info[s][0] < source_height && stacks_checked[s] != 1) {
                            stack_source_found = true;
                            stack_options_empty.clear();
                            stack_options_empty.add(s);
                            source_height = s_info[s][0];
                        } else if (s_info[s][1] == 0 && s_info[s][0] == source_height && stacks_checked[s] != 1) {
                            stack_options_empty.add(s);
                        }
                    }
                    if (stack_options_empty.size() != 0 && (stack_options_empty.size() == 1 || !consider_time)) {
                        W_stack_source = stack_options_empty.first();
                    } else if (stack_options_empty.size() != 0 && consider_time) { //Zeit von prev_block zu next_stack_R berücksichtigen
                        W_stack_source = Params.compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
                    }
                }
                stack_destination_found = true;
                while (stack_source_found && stack_destination_found && s_info[W_stack_source][0] != -1) {
                    all_W_stacks_exhausted = true;
                    for (int s = 0; s < stacks; s++) {
                        if (s != W_stack_source && s_info[s][1] == 0 && s_info[s][0] != tiers-1) {
                            all_W_stacks_exhausted = false;
                            s = stacks;
                        }
                    }
                    int W_stack_destination = 0;
                    if (!all_W_stacks_exhausted) {
                        stack_destination_found = false;
                        TreeSet<Integer> stack_options_W = new TreeSet<>();
                        for (int s = 0; s < stacks; s++) {
                            if (s_info[s][0] != tiers - 1 && s != W_stack_source && s_info[s][1] == 0) {
                                stack_destination_found = true;
                                stack_options_W.add(s);
                            }
                        }
                        if (stack_options_W.size() == 0) {
                            stacks_checked[W_stack_source] = 1;
                        } else if (stack_options_W.size() == 1 || !consider_time) {
                            W_stack_destination = stack_options_W.first();
                        } else if (consider_time) {
                            W_stack_destination = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
                        }
                    } else {
                        //denjenigen R stack mit der geringsten zeitlichen Entfernung zu stack source auswählen, da der Block sowieso wieder umgelagert werden muss
                        stack_destination_found = false;
                        TreeSet<Integer> stack_options_W = new TreeSet<>();
                        for (int s = 0; s < stacks; s++) {
                            if (s_info[s][0] != tiers - 1 && s != W_stack_source && s_info[s][1] == 1) {
                                stack_destination_found = true;
                                stack_options_W.add(s);
                            }
                        }
                        if (stack_options_W.size() == 0) {
                            stacks_checked[W_stack_source] = 1;
                        } else if (stack_options_W.size() == 1 || !consider_time) {
                            W_stack_destination = stack_options_W.first();
                        } else if (consider_time) {
                            W_stack_destination = Params.compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
                        }
                    }
                    if (stack_destination_found) {
                        int block = copy[W_stack_source][s_info[W_stack_source][0]][0] - 1;
                        int prio = c_info[block][2];
                        if (PreMarshalling.print_info) {
                            System.out.println("Create empty stack: Move containers in W to R!");
                        }
                        Relocation.relocate(c_info, s_info, block, prio, W_stack_destination, tiers, stacks_per_bay, multiple_bays);
                        if (s_info[W_stack_source][0] == -1) {
                            if (PreMarshalling.print_info) {
                                System.out.println("Create empty stack: Stack emptied!");
                            }
                            empty_stack = W_stack_source;
                            step_empty_stack = PreMarshalling.step;
                            stack_source_found = false;
                        }
                    } else {
                        stacks_checked[W_stack_source] = 1;
                        PreMarshalling.current_bay = BayInstance.copy_bay(Relocation.copy, PreMarshalling.current_bay, stacks, tiers);
                    }
                }
            }
        }
    }
}
