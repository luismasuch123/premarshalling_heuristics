import java.util.*;

public class Params {

    static int [][] bay_info = new int [5][2]; //1st/2nd and 3rd highest top stack due date value of not well located stacks (prio and stack) and -1 at position 4 and/or 5 if there are empty stacks
    static int [][] s_info; //highest tier, ordered
    static int [][] c_info; //stack, tier, prio, sorted
    static boolean sorted;

    //Jovanovic
    static int [][]  g_c_s; //number of containers above c in stack s
    static int [][] f_c_s; //containers above well located block c_a with larger or same due date value than c in destination stack s
    static int [][]nw_c_s; //well located containers that need to be relocated when c is moved to destination stack s
    static int [][] f_c_s_ext; //berücksichtigt zusätzlich zu f_c_s, dass Umlagerungen von bereits richtig platzierten Blöcken vermieden werden sollten
    static int [][] w_c_s; //Summe der Blöcke, die bei einer Umlagerung von c aus stack s in destination stack ss, aus s und ss heraus umgelagert werden müssen
    static int [] d_c; //d_c is the index of the stack that the lowest value of w_c_s for a block c and if tied the minimum distance to c

    //Huang
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
    //create empyt_stack
    static int empty_stack;
    static int step_empty_stack;

    //LB
    static int n_bx;
    static int n_gx;
    //sets
    static Set<Integer> S_M = new HashSet<>(); //Set of misoverlaid stacks.
    static Set<Integer> S_minM = new HashSet<>(); //Set of misoverlaid stacks with the minimum number of misoverlaying contaienrs. TODO: Berechnung
    static Set<Integer> S_N = new HashSet<>(); //Set of non-misoverlaid stacks.
    static Set<Integer> U = new HashSet<>(); //Set of stacks where the misoverlaying containers are "upside-down" sorted
    static Set<Integer> Us = new HashSet<>(); //Set of stacks that would be upside down if one misoverlaying containers was removed. We assume U C Us.
    //LB_F
    static int groups; //number of different prio-groups //Gruppe fängt bei 0 an und keine Gruppen dürfen ausgelassen werden.
    static int n_b; //number of badly placed items in L
    static int [] n_b_s; //number of badly placed items in stack s
    static int n_b_s_min;
    static int [][] n_g_s; //number of well placed items of all groups gs < g in stack s
    static int [] d_g; //number of ll badly placed items of group g / demand of group g
    static int [] d_g_cum; //cumulative demand of group g
    static int [] s_p_g; //number of all potential supply slots of group g / potential supply of group g
    static int [] s_p_g_cum; //cumulative potential supply of group g
    static int [] d_s_g_cum; //cumulative demand surplus of group g
    static int [] d_s_g_cum_max; //value, index
    //IBF
    static boolean case_1;
    static boolean case_2a;
    static boolean case_2b;
    static boolean case_3;
    static boolean case_4;
    static int [] n_s; //Number of containers in stacks s.
    static int [] n_M_s; //Number of misoverlaying containers in stack s.
    static int [] n_N_s; //Number of non-misoverlaying containers in stack s.
    static int n_M; //Total number of misoverlaying containers in the bay.
    static int h_M; //Minimum number of misoverlaying containers in the misoverlaid stacks.
    static int [] g_top_s; //Group value of the topmost container in stack s. If stack s is empty, g_top_s = groups
    static int [] g_sec_s; //Group value of the second topmost container in stack s.
    static int [] c_top_s; //Topmost container in stack s. If stack s is empty, c_top_s = leere Menge.
    static int [] m_s; //Largest group value of the misoverlaying conntainers in stack s in S_M. If s in U, m_s = g_top_s
    static int [][] m_i_s; //The group value of the ith largest misoverlaying group value in stack s
    static int [] w_s; //Smallest group value of non-misoverlaying containers in stack s. If stack s is empty, w_s = groups. If s in S_N, w_s = g_top_s.
    static int [] w_sec_s; //Second smallest group value of non-misoverlaying containers in stack s. If all the non-misoverlaying containers in stack s have the same group, w_sec_s = groups
    static int [] g_X_s; //Minimum top group value of the non-misoverlaid stack necessary for repairing stack s in Us with BG moves to the stack and one BX move to another stack.
    static int [] n_BG_s; //Minimum number of non-misoverlaid stacks necessary for repairing stack s with only BG moves.
    static int [][] g_BG_si; //Minimum group value of the topmost conatainer of the ith non-misoverlaid stack for repairing stack s.

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

    public static void compute__w_c_s(int stacks, int containers) {
        w_c_s = new int[containers][stacks];
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
        f_c_s_ext = new int[containers][stacks];
        for (int c = 0; c < containers; c++) {
            for (int s = 0; s < stacks; s++) {
                f_c_s_ext[c][s] = f_c_s[c][s] + nw_c_s[c][s];
            }
        }
    }

    public static int[] compute_fr_c_s(int [][][] copy, int[] fr_c_s, int[] d_c, Object[] next_options) {
        //wenn es zwei empty stacks gibt, kann es keine forced relocations geben
        if (! (bay_info[3][1] == -1 && bay_info[4][0] == -1)) {
            for (Object next_option : next_options) {
                int c = (int) next_option;
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

    public static void compute_d_c(int [][][] copy, int stacks, int stacks_per_bay, int tiers, int containers, boolean consider_time) {
        d_c = new int [containers];
        for(int c = 0; c < containers; c++) {
            //Liste mit den Indizes der niedrigsten w_c_s erstellen, um dann Distanzen zu vergleichen
            int lowest_w_c_s = 100000;
            Set<Integer> indices = new HashSet<>();
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
                    double time_to_current_stack = get_time(c_info[c][0], indice, c_info[c][1], s_info[indice][0] - f_c_s[c][indice], tiers, stacks_per_bay);
                    if (time_to_current_stack < time_to_stack) {
                        d_c[c] = indice;
                        time_to_stack = time_to_current_stack;
                    }
                }
            } else {
                TreeSet<Integer> indices_sorted = new TreeSet(indices);
                d_c[c] = indices_sorted.first();
            }
        }
    }

    public static double get_time(int stack_from, int stack_to, int tier_from, int tier_to, int tiers, int stacks_per_bay) {
        //TODO: Beschleunigung berücksichtigen?
        return (double) Math.abs(stack_from/stacks_per_bay - stack_to/stacks_per_bay) * 2 * 1.875 + (double) Math.abs(stack_from % stacks_per_bay - stack_to % stacks_per_bay) * 2 * 2.4 + (double) ((tiers - tier_from) + (tiers - tier_to)) * 2 * 15.0;
    }

    public static void compute__f_c_s__nw_c_s(int [][][] copy, int stacks, int tiers, int containers) {
        f_c_s = new int[containers][stacks];
        nw_c_s = new int[containers][stacks];
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

    public static void compute__c_info__s_info__g_c_s(int [][][] copy, int stacks, int tiers, int containers) {
        g_c_s = new int[containers][stacks];
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

    public static void complete_high_R_stacks(int [][][] copy, int [][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        beta_h = beta * tiers;
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
                next_stack_R = compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                        next_stack_W = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R, s_info[next_stack_R][0], 0);
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
                next_stack_R = compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                        next_stack_W = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R, s_info[next_stack_R][0], 0);
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
        /*
        boolean next_stack_R_low_found = true;
        next_stack_R_low_checked = new int [stacks];
        deconstruct_low_R_stack = false;

         */

        boolean next_stack_R_low_found = false;
        //verhindern, dass deconstructing bei allen low_R stacks nicht erfolgreich ist, sowie keine UMlagerungen stattfinden und im nächsten Durchgang erneut durchgeführt wird (loop)
        if (PreMarshalling.step == 1) {
            next_stack_R_low_found = true;
        } else {
            if (PreMarshalling.step ==200714) {
                int d = 6;
            }
            if (PreMarshalling.relocation_count_current == Relocation.relocations_count) {
                for (int s = 0; s < stacks; s++) {
                    if (next_stack_R_low_checked[s] == 0 && s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_low_checked[s] == 0 && s != c_info[Relocation.prev_block][0]) {
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
                next_stack_R_low = compute_nearest_stack(stack_options_R_low, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                        next_stack_R_deconstruct = compute_nearest_stack(stack_options_R, tiers, stacks_per_bay, next_stack_R_low, s_info[next_stack_R_low][0], 0);
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

                        Params.complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
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
                                next_stack_W_deconstruct = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, next_stack_R_low, s_info[next_stack_R_low][0], 0);
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
                                if (Relocation.prev_block == 7) {
                                    int d = 9;
                                }
                                Params.complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
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
            empty_stack = compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                    //TODO: brauche ich erste Bedingung?
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
                    }//TODO: sicherstellen, dass s_info[empty_stack][0] != -1
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
                    W_block = compute_nearest_stack(W_options, tiers, stacks_per_bay, empty_stack, s_info[empty_stack][0], 1);
                    int W_block_prio = c_info[W_block][2];
                    Relocation.relocate(c_info, s_info, W_block, W_block_prio, empty_stack, tiers, stacks_per_bay, multiple_bays);
                }
            }
        } else if (!empty_stack_found && PreMarshalling.relocation_count_current == Relocation.relocations_count && step_empty_stack + 1 == PreMarshalling.step) {
            if (beta < 1) {
                beta += 0.1;
                System.out.println("Beta increased by 0.1");
            } else {
            System.out.println("No solution found!");
            PreMarshalling.solution_found = false;
            //TODO: vermerken, dass keine Lösung gefunden wurde
            System.exit(0);
            }
        }
    }

    public static void create_empty_stack(int[][][] copy, int[][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        //wenn kein einzelner empty stack erzeugt werden kann, weil keine destination stacks mit prio <= prio von Block aus source vorhanden, dann Block aus source trotzdem umlagern
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
                W_stack_source = compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                    W_stack_destination = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
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
                    stacks_checked[W_stack_source] = 1; //TODO: überflüssig?
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
                        W_stack_source = compute_nearest_stack(stack_options_empty, tiers, stacks_per_bay, c_info[Relocation.prev_block][0], s_info[c_info[Relocation.prev_block][0]][0], 0);
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
                            W_stack_destination = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
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
                            W_stack_destination = compute_nearest_stack(stack_options_W, tiers, stacks_per_bay, W_stack_source, s_info[W_stack_source][0], 0);
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

    public static void compute_params_Jovanovic(int[][][] copy, int stacks, int stacks_per_bay, int tiers, int containers, boolean consider_time) {
        compute__c_info__s_info__g_c_s(copy, stacks, tiers, containers);
        compute__bay_info(copy, stacks);
        compute__f_c_s__nw_c_s(copy, stacks, tiers, containers);
        compute__f_c_s_ext(stacks, containers);
        compute__w_c_s(stacks, containers);
        compute_d_c(copy, stacks, stacks_per_bay, tiers, containers, consider_time);
    }

    public static int compute_params_LB(int [][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers) {
        //TODO: Hat Anzahl groups Auswirkung auf Parameter bzw. hat irgendein ein array die Länge groups?
        int LB_F = compute__LB_F(initial_bay, stacks, tiers);

        int IBF_0 = compute__IBF_0(initial_bay, stacks, tiers, true);

        compute__params_IBF(initial_bay, stacks, tiers);

        int IBF_1 = compute__IBF_1(IBF_0, stacks, tiers);

        int IBF_2 = compute__IBF_2(LB_F, IBF_0, IBF_1, stacks, tiers);

        int IBF_3 = compute__IBF_3(LB_F, IBF_2);

        int IBF_4 = 0;

        switch (PreMarshalling.lower_bound_method) {
            case "LB_F" -> {
                return LB_F;
            }
            case "IBF_0" -> {
                return IBF_0;
            }
            case "IBF_1" -> {
                return IBF_1;
            }
            case "IBF_2" -> {
                return IBF_2;
            }
            case "IBF_3" -> {
                return IBF_3;
            }
            case "IBF_4" -> {
                return IBF_4;
            }
        }

        return compute__n_m(stacks, tiers);
    }

    private static void compute__params_IBF(int[][][] initial_bay, int stacks, int tiers) {
        n_s = new int[stacks];
        n_M_s = new int[stacks];
        n_N_s  = new int[stacks];
        n_M = 0;
        h_M = 1000000;
        g_top_s = new int[stacks];
        g_sec_s = new int[stacks];
        c_top_s = new int[stacks];
        m_s = new int[stacks];
        m_i_s = new int[stacks][tiers-1];
        w_s = new int[stacks];
        w_sec_s = new int[stacks]; //gleich 0, wenn weniger als 2 geordnete Blöcke (n_N_s < 2)
        g_X_s = new int[stacks];
        n_BG_s = new int[stacks];
        g_BG_si = new int[stacks][tiers];

        for (int s = 0; s < stacks; s++) {
            Set<Integer> largest_misoverlaying_group_values = new TreeSet<>(); //sorts the misoverlaying group values in a stack s, so that they can be added to m_i_s afterwards
            Set<Integer> minimum_group_values_for_repairing = new TreeSet<>();
            m_s[s] = 0;
            w_s[s] = 100000;
            boolean all_non_misoverlaying_containers_same_group = false;
            int group_t_current = 0;
            int group_t_last = 0;
            boolean misoverlaid_upside_down = true;
            boolean misoverlaid_upside_down_except_one = true;
            if (s_info[s][0] != -1) {
                g_top_s[s] = initial_bay[s][s_info[s][0]][1];
                c_top_s[s] = initial_bay[s][s_info[s][0]][0];
                for (int t = 0; t <= s_info[s][0]; t++) {
                    n_s[s]++;
                    if (initial_bay[s][t][2] == 1) {
                        if (n_N_s[s] == 0) {
                            all_non_misoverlaying_containers_same_group = true;
                        }
                        n_N_s[s]++;
                        if (initial_bay[s][t][1] < w_s[s]) {
                            w_sec_s[s] = w_s[s];
                            w_s[s] = initial_bay[s][t][1];
                            if (n_N_s[s] > 1) { //erst ab dem zweiten container in einem stack, der nicht geordnet ist, kann entschieden werden, dass die Container nicht der gleichen Gruppe angehören
                                all_non_misoverlaying_containers_same_group = false;
                            }
                        }
                    } else {
                        n_M_s[s]++;
                        n_M++;
                        largest_misoverlaying_group_values.add(initial_bay[s][t][1]);
                        group_t_current = initial_bay[s][t][1];
                        if (group_t_last > group_t_current && t != tiers-1) {
                            if (n_M_s[s] > 2 && !(t == s_info[s][0] && misoverlaid_upside_down) && !(initial_bay[s][t + 1][1] >= group_t_last && misoverlaid_upside_down)) {
                                misoverlaid_upside_down_except_one = false;
                            } else {
                            group_t_last = group_t_current;
                            }
                            misoverlaid_upside_down = false;
                            minimum_group_values_for_repairing.add(group_t_current);
                        } else {
                            group_t_last = group_t_current;
                        }
                    }
                }
            } else {
                g_top_s[s] = groups;
                c_top_s[s] = 0;
                w_s[s] = groups;
            }
            if (all_non_misoverlaying_containers_same_group && n_N_s[s] > 1) { //TODO: soll dies nur gelten, wenn mind. 2 geordnete Blöcke vorhanden sind
                w_sec_s[s] = groups;
            }
            if (s_info[s][0] <= 0) {
                g_sec_s[s] = groups;
            } else {
                g_sec_s[s] = initial_bay[s][s_info[s][0]-1][1];
            }
            //w_sec_s
            if (n_N_s[s] < 2) {
                w_sec_s[s] = 0;
            }
            if (s_info[s][1] == 0) {
                S_M.add(s);
                if (n_M_s[s] < h_M) {
                    h_M = n_M_s[s];
                }
                if (misoverlaid_upside_down) {
                    U.add(s);
                    Us.add(s);
                } else if (misoverlaid_upside_down_except_one) {
                    Us.add(s);
                }
                if (Us.contains(s)) {
                    //TODO: Muss BX move durchgeführt werden, oder wird g_X_s[s] auf belegt, wenn nur BG moves?
                    //TODO: g_X_s: Wenn n_M_s[s] gleich 2 ist, welchen Block bewege ich dann auf den non-misoverlaid stack?
                    if (n_M_s[s] > 2) {
                        if (initial_bay[s][s_info[s][0]][1] > initial_bay[s][s_info[s][0] - 1][1]) {
                            g_X_s[s] = initial_bay[s][s_info[s][0]][1];
                        } else if (initial_bay[s][s_info[s][0]][1] > initial_bay[s][s_info[s][0] - 2][1]) {
                            g_X_s[s] = initial_bay[s][s_info[s][0]][1];
                        } else if (initial_bay[s][s_info[s][0] - 1][1] > initial_bay[s][s_info[s][0] - 2][1]) {
                            g_X_s[s] = initial_bay[s][s_info[s][0] - 1][1];
                        }
                    } else if (n_M_s[s] == 2){
                        if (initial_bay[s][s_info[s][0]][1] > initial_bay[s][s_info[s][0] - 1][1]) {
                            g_X_s[s] = initial_bay[s][s_info[s][0]][1];
                        } else if (initial_bay[s][s_info[s][0]][1] < initial_bay[s][s_info[s][0] - 1][1]) {
                            g_X_s[s] = initial_bay[s][s_info[s][0] - 1][1]; //TODO: wenn man die kleinere Prio /den oberen Block bewegen würde hätte man es erstmal einfacher, wäre aber auf lange Sicht wohl weniger sinnvoll
                        }
                    } else {
                        g_X_s[s] = initial_bay[s][s_info[s][0]][1];
                    }
                }
                TreeSet<Integer> largest_misoverlaying_group_values_Reverse = (TreeSet<Integer>) ((TreeSet<Integer>) largest_misoverlaying_group_values).descendingSet();
                m_s[s] = largest_misoverlaying_group_values_Reverse.first();
                for (Integer j: largest_misoverlaying_group_values_Reverse) {
                    m_i_s[s][indexOf(largest_misoverlaying_group_values_Reverse, j)] = j;
                }
                int [][] non_misoverlaid_stacks_necessary = new int [stacks][tiers-1];
                boolean new_stack_because_not_upside_down = false;
                for (int t = s_info[s][0]; t > 0 && initial_bay[s][t][2] == 0; t--) {
                    for (int ss = 0; ss < stacks; ss++) {
                        for (int tt = 0; tt < tiers-1; tt++) {
                            while(tt != tiers-2 && non_misoverlaid_stacks_necessary[ss][tt+1] != 0) {
                                tt++;
                            }
                            if (tt == 0 && non_misoverlaid_stacks_necessary[ss][tt] == 0) {
                                non_misoverlaid_stacks_necessary[ss][tt] = initial_bay[s][t][1];
                                tt = tiers;
                                ss = stacks;
                                new_stack_because_not_upside_down = false;
                            } else if (initial_bay[s][t][1] <= non_misoverlaid_stacks_necessary[ss][tt] && !new_stack_because_not_upside_down) {
                                //TODO: überprüfen, dass unterhalb von Block keiner ist, der die Bedingung ebenfalls erfüllt und größer als höherer Gruppe als der Block angehört
                                for(int ttt = t-1; ttt > 0 && initial_bay[s][ttt][2] == 0; ttt--) {
                                    if (initial_bay[s][ttt][1] <= non_misoverlaid_stacks_necessary[ss][tt] && initial_bay[s][ttt][1] > initial_bay[s][t][1]) {
                                        new_stack_because_not_upside_down = true;
                                        ttt = 0;
                                        tt = tiers;
                                    }
                                }
                                if (!new_stack_because_not_upside_down) {
                                    non_misoverlaid_stacks_necessary[ss][tt + 1] = initial_bay[s][t][1];
                                    tt = tiers;
                                    ss = stacks;
                                }
                            } else if (new_stack_because_not_upside_down && tt > 0 && initial_bay[s][t][1] <= non_misoverlaid_stacks_necessary[ss][tt]) {
                                non_misoverlaid_stacks_necessary[ss][tt+1] = initial_bay[s][t][1];
                                new_stack_because_not_upside_down = false;
                                tt = tiers;
                                ss = stacks;
                            } else if (non_misoverlaid_stacks_necessary[ss][tt] == 0) {
                                tt = tiers;
                            } else if (initial_bay[s][t][1] > non_misoverlaid_stacks_necessary[ss][tt]) {
                                tt = tiers;
                            }
                        }
                    }
                }
                Set<Integer> minimum_group_value_ith_non_misoverlaid_stack = new TreeSet<>();
                for (int ss = 0; ss < stacks; ss++) {
                    if (non_misoverlaid_stacks_necessary[ss][0] != 0) {
                        n_BG_s[s] += 1;
                        minimum_group_value_ith_non_misoverlaid_stack.add(non_misoverlaid_stacks_necessary[ss][0]);
                    }
                }
                TreeSet<Integer> minimum_group_value_ith_non_misoverlaid_stack_Reverse = (TreeSet<Integer>) ((TreeSet<Integer>) minimum_group_value_ith_non_misoverlaid_stack).descendingSet();
                for (Integer group_value: minimum_group_value_ith_non_misoverlaid_stack_Reverse) {
                    g_BG_si[s][indexOf(minimum_group_value_ith_non_misoverlaid_stack_Reverse, group_value)] = group_value;
                }

                minimum_group_values_for_repairing.add(group_t_last); //da last nur geupdated wird, wenn der upside-down-Teil durchlaufen wird, kann über last immer der fehlende minimum group value ermittelt werden
                TreeSet<Integer> minimum_group_values_for_repairing_Reverse = (TreeSet<Integer>) ((TreeSet<Integer>) largest_misoverlaying_group_values).descendingSet();
                for (Integer j: minimum_group_values_for_repairing_Reverse) {
                    m_i_s[s][indexOf(minimum_group_values_for_repairing_Reverse, j)] = j;
                }
            } else {
                S_N.add(s);
            }
        }
    }

    private static int indexOf(Set<Integer> set, Integer element) {
        // Step 1: Convert TreeSet to ArrayList or
        // LinkedList
        List<Integer> list = new ArrayList<>(set);

        // Step 2: Use the indexOf method of the List
        return list.indexOf(element);
    }

    private static int compute__IBF_3(int LB_F, int IBF_2) {
        if (LB_F == IBF_2) { //IBF_3-Bedingung
            return LB_F + 1;
        } else {
            return IBF_2;
        }
    }

    private static int compute__IBF_2(int LB_F, int IBF_0, int IBF_1, int stacks, int tiers) {
        int IBF_2 = IBF_1;
        if (case_1 || case_2a || case_2b || case_3 || case_4) {
            if (case_1) {
                //proposition 4
                //Bedingung IBF_1 = IBF_0 + 1 in dieser if-Bedingung automatisch erfüllt //condition 2
                int min_m_ss = 1000000;
                for (int s: U) {
                    if (m_s[s] < min_m_ss) {
                        min_m_ss = m_s[s];
                    }
                }
                int max_w_s = 0;
                for (int s = 0; s < stacks; s++) {
                    if (n_M_s[s] == h_M + 1 && w_s[s] > max_w_s) {
                        max_w_s = w_s[s];
                    }
                }
                if (min_m_ss > max_w_s) { //condition 3
                    int max_w_sec_s = 0;
                    for (int s : S_minM) {
                        if (w_sec_s[s] > max_w_sec_s) {
                            max_w_sec_s = w_sec_s[s];
                        }
                    }
                    if (min_m_ss > max_w_sec_s) { //condition 4
                        int min_g_X_s = 100000;
                        for (int s: Us) {
                            if (g_X_s[s] < min_g_X_s) {
                                min_g_X_s = g_X_s[s];
                            }
                        }
                        max_w_s = 0;
                        for (int s: S_minM) {
                            if (w_s[s] > max_w_s) {
                                max_w_s = w_s[s];
                            }
                        }
                        if (min_g_X_s > max_w_s) { //condition 5
                            IBF_2 += 1;
                        }
                    }
                }
            }
            if (case_2b) {
                int s = 0; //index of non-misoverlaid stack
                for (int ss: S_N) {
                    s = ss;
                }
                //proposition 5
                if (IBF_1 == IBF_0 + 1) { //condition 2
                    int min_m_s = 100000;
                    for (int ss: U) {
                        if (m_s[ss] < min_m_s) {
                            min_m_s = m_s[ss];
                        }
                    }
                    if (min_m_s > w_sec_s[s]) { //condition 3
                        int min_g_X_s = 100000;
                        for (int ss: Us) {
                            if (g_X_s[ss] < min_g_X_s) {
                                min_g_X_s = g_X_s[ss];
                            }
                        }
                        if (min_g_X_s > w_s[s]) { //condition 4
                            IBF_2 += 1;
                        }
                    }
                }
                //proposition 6
                if (U.size() == 1) { //condition 2
                    boolean condition_2 = false;
                    int s_upside_down = 0;
                    for (int ss: U) {
                        if (g_top_s[ss] <= w_s[s]) { //condition 2
                            condition_2 = true;
                            s_upside_down = ss;
                        }
                    }
                    if (condition_2) { //condition 2
                        if (IBF_1 == IBF_0) { //condition 3
                            boolean condition_4 = true;
                            int [] w_s_s = new int [stacks];
                            for (int sss = 0; sss < stacks; sss++) {
                                if (n_gx > 0 && n_s[s] + n_M_s[s_upside_down] < tiers) {
                                    w_s_s[sss] = w_sec_s[s_upside_down];
                                } else {
                                    w_s_s[sss] = w_s[s_upside_down];
                                }
                            }
                            for (int ss: S_M) {
                                if (ss != s_upside_down) {
                                    if (!(n_BG_s[ss] > 2 || g_BG_si[ss][0] > Math.max(w_s_s[s], w_s_s[s_upside_down]) || g_BG_si[ss][1] > Math.min(w_s_s[s], w_s_s[s_upside_down]))) {
                                        condition_4 = false;
                                    }
                                }
                            }
                            if (condition_4) { //condition 4
                                IBF_2 += 1;
                            }
                        }
                    }
                }
            }
            if (case_3) {
                //proposition 7
                int s_1 = 0;
                int s_2 = 0;
                for (int s: S_N) { //wegen condition 1 ist S_N.size() == 2 sichergestellt
                    if (indexOf(S_N, s) == 0) {
                        s_1 = s;
                    } else {
                        s_2 = s;
                    }
                }
                int w_ss_s_1;
                int w_ss_s_2;
                if (w_s[s_1] < w_s[s_2] && n_s[s_2] == tiers) {
                    w_ss_s_1 = w_s[s_1];
                    w_ss_s_2 = -10000000;
                } else {
                    if (n_s[s_2] < tiers) {
                        w_ss_s_1 = w_sec_s[s_1];
                    } else {
                        w_ss_s_1 = w_s[s_1];
                    }
                    if (n_s[s_1] < tiers && w_s[s_1] == w_s[s_2]) {
                        w_ss_s_2 = w_sec_s[s_2];
                    } else {
                        w_ss_s_2 = w_s[s_2];
                    }
                }
                if (IBF_1 == IBF_0) { //condition 2
                    boolean condition_3 = true;
                    for (int s: S_M) {
                        if (!(n_BG_s[s] > 2 || g_BG_si[s][0] > Math.max(w_ss_s_1, w_ss_s_2) || g_BG_si[s][1] > Math.min(w_ss_s_1, w_ss_s_2))) {
                            condition_3 = false;
                        }
                    }
                    if (condition_3) { //condition 3
                        IBF_2 += 1;
                    }
                }
            }
            if (case_4) {
                //proposition 8
                int s = 0;
                for (int ss: S_N) {
                    s = ss;
                }
                if (n_gx == 0 && IBF_1 == IBF_0 + 1) { //condition 2 & 3
                    int min_g_X_s = 100000;
                    for (int ss: Us) {
                        if (g_X_s[ss] < min_g_X_s) {
                            min_g_X_s = g_X_s[ss];
                        }
                    }
                    if (min_g_X_s > w_s[s]) { //condition 4
                        IBF_2 += 1;
                    }
                }
            }
        }
        return IBF_2;
    }

    private static int compute__IBF_1(int IBF_0, int stacks, int tiers) {
        //IBF_1
        case_1 = true;
        case_2a = false;
        case_2b = false;
        case_3 = false;
        case_4 = false;
        int stacks_ordered = 0; //TODO: kann ersetzt werden durch size(S_N)
        int ordered_stacks_not_full = 0;

        for (int s = 0; s < stacks; s++) {
            if (s_info[s][1] == 1) {
                case_1 = false;
                stacks_ordered++;
                if (s_info[s][0] != tiers - 1) {
                    ordered_stacks_not_full++;
                }
            } else if (s_info[s][1] == 1 && s_info[s][0] != tiers - 1) {
                case_2a = true;
            } else if (s_info[s][0] == tiers - 1) {
                case_2b = true;
            }
        }
        if (stacks_ordered == 2 && ordered_stacks_not_full >= 1) {
            case_3 = true;
        }
        if (n_gx == 0 && ordered_stacks_not_full == 1) {
            case_4 = true;
        }
        if (case_1 || case_2a || case_2b || case_3 || case_4) {
            return IBF_0 + 1;
        } else {
            return IBF_0;
        }
    }

    private static int compute__IBF_0(int[][][] initial_bay, int stacks, int tiers, boolean IBF_0_extension) {
        //wenn alle geordneten stacks voll sind, kann wie im Fall, dass alle stacks ungeordnet sind, n_b_s_min addiert werden
        compute__n_b__n_b_s(initial_bay, stacks, tiers, IBF_0_extension);
        return compute__n_m(stacks, tiers);
    }

    private static int compute__LB_F(int[][][] initial_bay, int stacks, int tiers) {
        compute__groups(initial_bay, stacks, tiers);
        compute__n_b__n_b_s(initial_bay, stacks, tiers, false);
        compute__n_g_s(initial_bay, stacks, tiers);
        compute__d_g__d_g_cum(initial_bay, stacks, tiers);
        compute__s_p_g__s_p_g_cum(initial_bay, stacks, tiers);
        compute__d_s_g_cum(initial_bay, stacks, tiers);

        return compute__n_m(stacks, tiers);
    }

    private static int compute__n_m(int stacks, int tiers) {
        n_bx = n_b + n_b_s_min;

        int n_s_gx = Math.max(0, d_s_g_cum_max[0] / tiers);
        //stacks werden nach n_g_s aufsteigend sortiert , um dann die n_g_s in den ersten n_s_gx stacks aufzusummieren
        n_gx = 0; //Summe n_g_s über die ersten n_s_gx stacks nach Sortierung
        int [] stacks_checked = new int [stacks];
        for (int i = 0; i < n_s_gx; i++) {
            int n_g_s_min_val = 100000;
            int n_g_s_min_index = 0;
            for (int s = 0; s < stacks; s++) {
                if (n_g_s[d_s_g_cum_max[1]][s] <= n_g_s_min_val && stacks_checked[s] == 0) {
                    n_g_s_min_val = n_g_s[d_s_g_cum_max[1]][s];
                    n_g_s_min_index = s;
                }
            }
            n_gx += n_g_s_min_val;
            stacks_checked[n_g_s_min_index] = 1;
        }

        return n_bx + n_gx;
    }

    private static void compute__d_s_g_cum(int[][][] initial_bay, int stacks, int tiers) {
        d_s_g_cum = new int [groups];
        d_s_g_cum_max = new int [2];
        for (int g = 0; g < groups; g++) {
            d_s_g_cum[g] = d_g_cum[g] - s_p_g_cum[g];
            if (d_s_g_cum[g] > d_s_g_cum_max[0]) {
                d_s_g_cum_max[0] = d_s_g_cum[g];
                d_s_g_cum_max[1] = g;
            }
        }
    }

    private static void compute__s_p_g__s_p_g_cum(int[][][] initial_bay, int stacks, int tiers) {
        s_p_g = new int [groups];
        s_p_g_cum = new int [groups];
        int empty_stacks = 0;
        for (int g = 0; g < groups; g++) {
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][0] != -1) {
                    for (int t = 0; t <= s_info[s][0]; t++) {
                        if (initial_bay[s][t][2] == 0 && initial_bay[s][t-1][1] == g+1) {
                            s_p_g[g] += tiers - t;
                            t = tiers;
                        } else if (initial_bay[s][t][2] == 0) {
                            t = tiers;
                        }
                    }
                } else {
                    empty_stacks++;
                }
            }
        }
        for (int g = 0; g < groups; g++) {
            for (int gg = g; gg < groups; gg++) {
                s_p_g_cum[g] += s_p_g[gg];
            }
            s_p_g_cum[g] += tiers * empty_stacks;
        }

    }

    private static void compute__d_g__d_g_cum(int[][][] initial_bay, int stacks, int tiers) {
        d_g = new int [groups];
        d_g_cum = new int [groups];
        for (int g = 0; g < groups; g++) {
            for (int s = 0; s < stacks; s++) {
                if (s_info[s][1] == 0) {
                    for (int t = 0; t <= s_info[s][0]; t++) {
                        if (initial_bay[s][t][1] == g + 1 && initial_bay[s][t][2] == 0) {
                            d_g[g] += 1;
                        }
                    }
                }
            }
        }
        for (int g = 0; g < groups; g++) {
            for (int gg = g; gg < groups; gg++) {
                d_g_cum[g] += d_g[gg];
            }
        }
    }

    private static void compute__groups(int[][][] initial_bay, int stacks, int tiers) {
        //TODO: Wenn eine Gruppe nicht vorkommt, dann ist Berechnung falsch
        //TODO: Eine Konvention festlegen: Fängt bei 0 oder 1 an, können Gruppen ausgelassen werden, usw.
        //Gruppe fängt bei 0 an und keine Gruppen dürfen ausgelassen werden.
        groups = 0;
        for (int s = 0; s < stacks; s++) {
            for (int t = 0; t <= s_info[s][0]; t++) {
                if (initial_bay[s][t][1] > groups) {
                    groups = initial_bay[s][t][1];
                }
            }
        }
    }

    private static void compute__n_g_s(int [][][] initial_bay, int stacks, int tiers) {

        n_g_s = new int [groups][stacks];
        for (int g = 0; g < groups; g++) {
            for (int s = 0; s < stacks; s++) {
                for (int t = 0; t <= s_info[s][0]; t++) {
                    if (initial_bay[s][t][1] < g+1 && initial_bay[s][t][2] == 1) {
                        n_g_s[g][s] += 1;
                    }
                }
            }
        }
    }

    private static void compute__n_b__n_b_s(int [][][] initial_bay, int stacks, int tiers, boolean IBF_0_extension) {
        n_b = 0;
        n_b_s = new int[stacks];
        n_b_s_min = 100000;
        for (int s = 0; s < stacks; s++) {
            if (s_info[s][1] == 0) {
                for (int t = 0; t <= s_info[s][0]; t++) {
                    if (initial_bay[s][t][2] == 0) {
                        n_b += 1;
                        n_b_s[s] += 1;
                    }
                }
                if (n_b_s[s] < n_b_s_min) {
                    n_b_s_min = n_b_s[s];
                }
            } else if (IBF_0_extension && s_info[s][0] != tiers-1) {
                n_b_s_min = 0;
            } else if (!IBF_0_extension){
                n_b_s_min = 0;
            }
        }
    }
}
