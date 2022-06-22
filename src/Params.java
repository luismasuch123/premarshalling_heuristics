import java.util.*;

public class Params {

    static int [][] bay_info = new int [5][2]; //1st/2nd and 3rd highest top stack due date value of not well located stacks (prio and stack) and -1 at position 4 and/or 5 if there are empty stacks
    static int [][] s_info; //highest tier, ordered
    static int [][] c_info; //stack, tier, prio, sorted
    static boolean sorted;

    public static void check_sorted_pre(int [][][] initial_bay, int stacks, int tiers) {
        sorted = true;
        for (int s = 0; s < stacks; s++) {
            for (int t = 0; t < tiers; t++) {
                if (initial_bay[s][t][2] == 0) {
                    sorted = false;
                    t = tiers;
                }
            }
        }
        if (sorted) {
            if (PreMarshalling.print_info) {
                System.out.println("Bay ist bereits geordnet!");
            }
        }
    }

    public static void check_sorted(int stacks) {
        sorted = true;
        for (int s = 0; s < stacks; s++) {
            if (s_info[s][1] == 0) {
                sorted = false;
                s = stacks;
            }
        }
    }

    public static double get_time(int stack_from, int stack_to, int tier_from, int tier_to, int tiers, int stacks_per_bay) {
        //TODO: Beschleunigung berücksichtigen?
        return (double) Math.abs(stack_from/stacks_per_bay - stack_to/stacks_per_bay) * 2 * PreMarshalling.speed_bays + (double) Math.abs(stack_from % stacks_per_bay - stack_to % stacks_per_bay) * 2 * PreMarshalling.speed_stacks + (double) ((tiers - tier_from) + (tiers - tier_to)) * 2 * PreMarshalling.speed_tiers;
    }

    public static void compute__bay_info(int [][][] copy, int stacks) {
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

    public static void compute__c_info__s_info(int [][][] copy, int stacks, int tiers) {
        for(int s = 0; s < stacks; s++) {
            boolean highest_tier_found = false;
            boolean tier_ordered = true;
            for (int t = tiers-1; t >= 0; t--) {
                if (copy[s][t][0] != 0) {
                    c_info[copy[s][t][0]-1][0] = s;
                    c_info[copy[s][t][0]-1][1] = t;
                    c_info[copy[s][t][0]-1][2] = copy[s][t][1];
                    c_info[copy[s][t][0]-1][3] = copy[s][t][2];
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


    public static int compute_nearest_stack(TreeSet<Integer> stack_options, int tiers, int stacks_per_bay, int stack_from, int tier_from, int tier_to) {
        int next_stack = 0;
        double time_to_next_stack_R = 1000000;
        for (int stack : stack_options) {
            if (tier_to == 0) {
                tier_to = s_info[stack][0];
            } else if (tier_to == 1){
                tier_to = c_info[stack][1];
            }
            double time_to_next_stack_R_option = get_time(stack_from, stack, tier_from, tier_to, tiers, stacks_per_bay);
            if (time_to_next_stack_R_option < time_to_next_stack_R) {
                next_stack = stack;
                time_to_next_stack_R = time_to_next_stack_R_option;
            }
        }
        return next_stack;
    }

    public static int compute_nearest_stack(TreeSet<Integer> stack_options, int tiers, int stacks_per_bay, int stack_from, int tier_from, int tier_to, int [][] s_info_help) {
        int next_stack = 0;
        double time_to_next_stack_R = 1000000;
        for (int stack : stack_options) {
            if (tier_to == 0) {
                tier_to = s_info_help[stack][0];
            }
            double time_to_next_stack_R_option = get_time(stack_from, stack, tier_from, tier_to, tiers, stacks_per_bay);
            if (time_to_next_stack_R_option < time_to_next_stack_R) {
                next_stack = stack;
                time_to_next_stack_R = time_to_next_stack_R_option;
            }
        }
        return next_stack;
    }

    public static int indexOf(Set<Integer> set, Integer element) {
        // Step 1: Convert TreeSet to ArrayList or
        // LinkedList
        List<Integer> list = new ArrayList<>(set);

        // Step 2: Use the indexOf method of the List
        return list.indexOf(element);
    }
}
