import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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

    //LB
    static int n_m;
    static int n_bx;
    static int n_gx;

    static int groups; //number of different prio-groups
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
        boolean next_stack_R_low_found = true;
        int [] next_stack_R_low_checked = new int [stacks];

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
                } else if(s_info[s][0] != -1 && s_info[s][1] == 1 && s_info[s][0] < beta_h && s_info[s][0] != tiers - 1 && next_stack_R_low_checked[s] == 0 && s != c_info[Relocation.prev_block][0] && copy[s][s_info[s][0]][1] < next_stack_R_low_prio) {
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
                                int candidate_block = copy[next_stack_R_low][s_info[next_stack_R_low][0]][0] - 1;
                                Relocation.relocate(c_info, s_info, candidate_block, next_stack_R_low_prio, next_stack_W_deconstruct, tiers, stacks_per_bay, multiple_bays);
                                current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
                                PreMarshalling.relocations.addAll(PreMarshalling.relocations_on_hold);

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
            if (s_info[s][0] == -1) {
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
                    if (s_info[s][1] == 0 && s_info[empty_stack][0] == -1) {
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
                    } else if (s_info[s][1] == 0 && copy[s][s_info[s][0]][1] <= copy[empty_stack][s_info[empty_stack][0]][1]) {
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
        }
    }

    public static void create_empty_stack(int[][][] copy, int[][] c_info, int[][] s_info, int stacks, int stacks_per_bay, int tiers, boolean multiple_bays, boolean consider_time) {
        //wenn kein einzelner empty stack erzeugt werden kann, weil keine destination stacks mit prio <= prio von Block aus source vorhanden, dann Block aus source trotzdem umlagern
        boolean stack_source_found = true;
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
            boolean stack_destination_found = true;
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
                        stack_source_found = false;
                    } else {
                        all_W_stacks_exhausted = true;
                        for (int s = 0; s < stacks; s++) {
                            if (s != W_stack_source && s_info[s][1] == 0 && s_info[s][0] != tiers-1) {
                                all_W_stacks_exhausted = false;
                                s = stacks;
                            }
                        }
                    }
                } else {
                    if (s_info[W_stack_source][1] == 1 && all_W_stacks_exhausted) {
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
                if (!all_W_stacks_exhausted) {
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
                boolean stack_destination_found = true;
                while (stack_source_found && stack_destination_found && s_info[W_stack_source][0] != -1) {
                    //denjenigen R stack mit der geringsten zeitlichen Entfernung zu stack source auswählen, da der Block sowieso wieder umgelagert werden muss
                    stack_destination_found = false;
                    int W_stack_destination = 0;
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
        compute__groups(initial_bay, stacks, tiers);
        compute__n_b__n_b_s(initial_bay, stacks, tiers);
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
            d_g_cum[g] = d_g_cum[g] - s_p_g_cum[g];
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
        groups = 0;
        for (int s = 0; s < stacks; s++) {
            for (int t = 0; t <= s_info[s][0]; t++) {
                if (initial_bay[s][t][1] > groups) {
                    groups = initial_bay[s][t][1] - 1;
                }
            }
        }
    }

    private static void compute__n_g_s(int [][][] initial_bay, int stacks, int tiers) {

        n_g_s = new int [groups][stacks];
        for (int g = 0; g < groups; g++) {
            for (int s = 0; s < stacks; s++) {
                for (int t = 0; t <= s_info[s][0]; t++) {
                    if (initial_bay[s][t][1] < g+1) {
                        n_g_s[g][s] += 1;
                    }
                }
            }
        }
    }

    private static void compute__n_b__n_b_s(int [][][] initial_bay, int stacks, int tiers) {
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
            }
        }
    }
}
