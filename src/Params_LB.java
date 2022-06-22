import java.util.*;
public class Params_LB {

    static int n_bx;
    static int n_gx;
    static int n_s_gx;
    //sets
    static Set<Integer> S_M = new HashSet<>(); //Set of misoverlaid stacks.
    static Set<Integer> S_minM = new HashSet<>(); //Set of misoverlaid stacks with the minimum number of misoverlaying contaienrs.
    static Set<Integer> S_N = new HashSet<>(); //Set of non-misoverlaid stacks.
    static Set<Integer> U = new HashSet<>(); //Set of stacks where the misoverlaying containers are "upside-down" sorted
    static Set<Integer> Us = new HashSet<>(); //Set of stacks that would be upside down if one misoverlaying containers was removed. We assume U C Us.
    //LB_F
    static int groups; //number of different prio-groups //Gruppe fängt bei 0 an und keine Gruppen dürfen ausgelassen werden.
    static int n_b; //number of badly placed items in L
    static int [] n_b_s; //number of badly placed items in stack s
    static int n_b_s_min;
    static int [][] n_g_s; //number of well placed items of all groups gs < g in stack s
    static int [] d_g; //number of all badly placed items of group g / demand of group g
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
    //time
    static int lower_bound_moves; //LB Umlagerungen
    static double lower_bound_time; //LB benötigte Zeit für Umlagerungen
    static double t_min; //minimale Zeit, die für eine Umlagerung benötigt werden kann
    static int [][] highest_tier_to_move_blocking_containers_to;

    public static void compute__params_IBF(int[][][] initial_bay, int [][] s_info, int stacks, int tiers) {
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
            List<Integer> largest_misoverlaying_group_values = new ArrayList<>(); //sorts the misoverlaying group values in a stack s, so that they can be added to m_i_s afterwards
            m_s[s] = 0;
            w_s[s] = 100000;
            boolean all_non_misoverlaying_containers_same_group = false;
            int group_t_current;
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
            if (all_non_misoverlaying_containers_same_group && n_N_s[s] > 1) {
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
                            g_X_s[s] = initial_bay[s][s_info[s][0] - 1][1];
                        }
                    } else {
                        g_X_s[s] = initial_bay[s][s_info[s][0]][1];
                    }
                }
                m_s[s] = Collections.max(largest_misoverlaying_group_values);
                int size = largest_misoverlaying_group_values.size();
                for (int j = 0; j < size; j++) {
                    m_i_s[s][j] = Collections.max(largest_misoverlaying_group_values);
                    largest_misoverlaying_group_values.remove(Collections.max(largest_misoverlaying_group_values));
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
                List<Integer> minimum_group_value_ith_non_misoverlaid_stack = new ArrayList<>();
                for (int ss = 0; ss < stacks; ss++) {
                    if (non_misoverlaid_stacks_necessary[ss][0] != 0) {
                        n_BG_s[s] += 1;
                        minimum_group_value_ith_non_misoverlaid_stack.add(non_misoverlaid_stacks_necessary[ss][0]);
                    }
                }
                size = minimum_group_value_ith_non_misoverlaid_stack.size();
                for (int j = 0; j < size; j++) {
                    g_BG_si[s][j] = Collections.max(minimum_group_value_ith_non_misoverlaid_stack);
                    minimum_group_value_ith_non_misoverlaid_stack.remove(Collections.max(minimum_group_value_ith_non_misoverlaid_stack));
                }
            } else {
                S_N.add(s);
            }
        }
    }

    public static int compute__IBF_4(int [][][] initial_bay, int LB_F, int IBF_3, int stacks) {
        int IBF_4 = IBF_3;
        if (LB_F == IBF_3) {
            for (int g = 0; g < groups-1; g++) {
                if (d_s_g_cum[g+1] == 0) {
                    for (int gks = 0; gks <= g; gks++) {
                        for (int gkb = gks; gkb <= g; gkb++) {
                            int min_dirty_height = 1000000;
                            for (int gs = gks; gs <= gkb; gs++) {
                                Set<Integer> clean_supply_stacks = new HashSet<>();
                                Set<Integer> clean_demand_stacks = new HashSet<>();
                                Set<Integer> dirty_stacks = new HashSet<>();
                                boolean [] demand_stack = new boolean [stacks];
                                boolean [] supply_stack = new boolean[stacks];
                                int [] dirty_height = new int [stacks];
                                int [] demand = new int [stacks];
                                int [] supply = new int [stacks];
                                for (int s = 0; s < stacks; s++) {
                                    for (int i = 0; i < n_M_s[s] - 1; i++) {
                                        if (gs <= m_i_s[i][s] && m_i_s[i][s] <= gkb) {
                                            demand_stack[s] = true;
                                            i = n_M_s[s];
                                        }
                                    }
                                    if (gs <= w_s[s] && w_s[s] <= gkb) {
                                        supply_stack[s] = true;
                                    }
                                    if (demand_stack[s] && supply_stack[s]) {
                                        dirty_stacks.add(s);
                                    }  else if (demand_stack[s]) {
                                        clean_demand_stacks.add(s);
                                    } else if (supply_stack[s]) {
                                        clean_supply_stacks.add(s);
                                    }
                                    for (int t = 0; t < n_s[s]; t++) {
                                        if (initial_bay[s][t][1] == gs) {
                                            demand[s] += d_g[gs];
                                            supply[s] += s_p_g[gs];
                                            if (dirty_stacks.contains(s) && d_g[gs] > 0) {
                                                dirty_height[s] += 1; //TODO: total number of dirty demands in a dirty stack
                                            }
                                        }
                                    }
                                    if (dirty_height[s] < min_dirty_height) {
                                        min_dirty_height = dirty_height[s];
                                    }
                                }
                                boolean condition_1 = false;
                                boolean condition_2 = false;
                                boolean condition_3 = false;

                                for (int s = 0; s < stacks; s++) {
                                    if (dirty_stacks.contains(s) && min_dirty_height > supply[s]) {
                                        condition_1 = true;
                                    }
                                    if (clean_supply_stacks.size() == 1 && dirty_stacks.size() == 0 && clean_demand_stacks.contains(s)) {
                                        //condition 2
                                        int group_topmost_demand_container = 0;
                                        int maximum_group_demand_containers = 0;
                                        boolean topmost_demand_container_found = false;
                                        for (int t = n_s[s]; t >= 0; t--) {
                                            if (d_g[initial_bay[s][t][1]] > 0) {
                                                if (!topmost_demand_container_found) {
                                                    group_topmost_demand_container = initial_bay[s][t][1];
                                                    topmost_demand_container_found = true;
                                                }
                                                if (initial_bay[s][t][1] > maximum_group_demand_containers) {
                                                    maximum_group_demand_containers = initial_bay[s][t][1];
                                                }
                                            }
                                        }
                                        if (group_topmost_demand_container < maximum_group_demand_containers) {
                                            condition_2 = true;
                                        }

                                    }
                                    if (clean_supply_stacks.size() == 1 && dirty_stacks.contains(s)) {
                                        //condition 3
                                        condition_3 = true;
                                        int maximum_group_dirty_demand_containers = 0;
                                        int topmost_dirty_demand_containers = 0; //group
                                        boolean topmost_dirty_demand_containers_found = false;
                                        int topmost_non_misoverlaying_containers = 0; //group
                                        boolean topmost_non_misoverlaying_containers_found = false;
                                        for (int t = n_s[s]; t >= 0; t--) {
                                            if (d_g[initial_bay[s][t][1]] > 0) {
                                                if (!topmost_dirty_demand_containers_found) {
                                                    topmost_dirty_demand_containers = initial_bay[s][t][1];
                                                    topmost_dirty_demand_containers_found = true;
                                                }
                                                if (initial_bay[s][t][1] > maximum_group_dirty_demand_containers) {
                                                    maximum_group_dirty_demand_containers = initial_bay[s][t][1];
                                                }
                                            }
                                            if (initial_bay[s][t][2] == 1 && !topmost_non_misoverlaying_containers_found) {
                                                topmost_non_misoverlaying_containers = initial_bay[s][t][1];
                                                topmost_non_misoverlaying_containers_found = true;
                                            }
                                        }
                                        if (maximum_group_dirty_demand_containers <= topmost_dirty_demand_containers || maximum_group_dirty_demand_containers <= topmost_non_misoverlaying_containers) {
                                            condition_3 = false;
                                        }
                                    }
                                }
                                if (condition_1 || condition_2 || condition_3) {
                                    IBF_4 += 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        return IBF_4;
    }

    public static int compute__IBF_3(int [][][] initial_bay, int LB_F, int IBF_2, int stacks, int tiers) {
        if (LB_F == IBF_2 && n_gx > 0) { //IBF_3-Bedingung
            //es werden n_s_gx dummy stacks hinzugefügt, um zu prüfen, ob diese und die vorhandenen s in s_N ausreichen, um die misoverlaid stacks zu repairen
            //falls dies nicht möglich, wird eine weitere Umlagerung benötigt

            //Hilfsarray, um bei Umlagerungen oberste Gruppe in stack festzuhalten
            int [] g_top_s_help = new int [stacks + n_s_gx];
            for (int s = 0; s < g_top_s_help.length; s++) {
                if (s < stacks) {
                    g_top_s_help[s] = g_top_s[s];
                } else {
                    g_top_s_help[s] = groups;
                }
            }
            //Hilfsarray, um bei Umlagerungen Anzahl der ungeordneten Blöcke in stack festzuhalten
            int [] n_M_s_help = new int [stacks];
            for (int s: S_M) {
                n_M_s_help[s] = n_M_s[s];
            }
            //Hilfsarray, um bei Umlagerungen Anzahl der Blöcke in stack festzuhalten
            int [] n_s_help = new int [stacks + n_s_gx];
            for (int s = 0; s < n_s_help.length; s++) {
                if (s < stacks) {
                    n_s_help[s] = n_s[s];
                } else {
                    n_s_help[s] = 0;
                }
            }
            int blocks_still_misoverlaid_count = n_M;
            Set <Integer> stacks_still_misoverlaid = new HashSet<>(S_M);
            Set <Integer> stacks_not_misoverlaid = new HashSet<>(S_N);
            //dummy stacks zu geordneten stacks hinzugefügt
            for (int s = stacks; s < stacks + n_s_gx; s++) {
                stacks_not_misoverlaid.add(s);
            }
            boolean no_additional_relocation_needed = true;
            while (blocks_still_misoverlaid_count > 0 && no_additional_relocation_needed) {
                //größter group value unter allen topmost blocks in nicht geordneten stacks
                int[] biggest_group_value_and_stack_misoverlaid_top = new int[2]; //value, stack
                for (int s: stacks_still_misoverlaid) {
                    if (g_top_s_help[s] > biggest_group_value_and_stack_misoverlaid_top[0]) {
                        biggest_group_value_and_stack_misoverlaid_top[0] = g_top_s_help[s];
                        biggest_group_value_and_stack_misoverlaid_top[1] = s;
                    }
                }
                //den Block mit dem größten group value aller topmost blocks umlagern auf Block in S_N mit kleinstmöglicher Gruppendifferenz
                //wenn kein Stack bzw. Block gefunden wird, dann wird eine weitere Umlagerung benötigt (LB_F += 1)
                int destination_stack = 0;
                boolean destination_stack_found = false;
                int difference_group_values = 1000000;
                for (int s: stacks_not_misoverlaid) {
                    if (n_s_help[s] < tiers && biggest_group_value_and_stack_misoverlaid_top[0] < g_top_s_help[s] && (g_top_s_help[s] - biggest_group_value_and_stack_misoverlaid_top[0]) < difference_group_values) {
                        destination_stack = s;
                        destination_stack_found = true;
                        difference_group_values = g_top_s_help[s] - biggest_group_value_and_stack_misoverlaid_top[0];
                    }
                }
                if (destination_stack_found) {
                    int source_stack = biggest_group_value_and_stack_misoverlaid_top[1];
                    int source_group = biggest_group_value_and_stack_misoverlaid_top[0];
                    //Updates durchführen
                    n_M_s_help[source_stack]--;
                    n_s_help[source_stack]--;
                    n_s_help[destination_stack]++;
                    g_top_s_help[destination_stack] = source_group;
                    g_top_s_help[source_stack] = initial_bay[source_stack][n_s_help[source_stack] - 1][1];
                    if (initial_bay[source_stack][n_s_help[source_stack] - 1][2] == 1) {
                        stacks_still_misoverlaid.remove(source_stack);
                        stacks_not_misoverlaid.add(source_stack);
                    }
                    blocks_still_misoverlaid_count--;
                } else {
                    //wenn ungeordnete stacks nicht repaired werden können
                    no_additional_relocation_needed = false;
                }
            }
            if (!no_additional_relocation_needed) {
                return LB_F + 1;
            } else {
                return IBF_2;
            }
        } else {
            return IBF_2;
        }
    }

    public static int compute__IBF_2(int LB_F, int IBF_0, int IBF_1, int stacks, int tiers) {
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
                        if (LB_F == IBF_0) { //condition 3 //TODO: IBF_1 == IBF_0 geht gar nicht, wenn case 2b, ergibt IBF_0 == LB_F mehr Sinn?
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
                    if (Params.indexOf(S_N, s) == 0) {
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
                if (LB_F == IBF_0) { //condition 2 //TODO: IBF_1 == IBF_0 kann mit case_3 nicht true sein
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

    public static int compute__IBF_1(int IBF_0, int stacks, int tiers) {
        //IBF_1
        case_1 = true;
        case_2a = false;
        case_2b = false;
        case_3 = false;
        case_4 = false;
        int stacks_ordered = S_N.size();
        int ordered_stacks_not_full = 0;

        for (int s = 0; s < stacks; s++) {
            if (S_N.contains(s)) {
                case_1 = false;
                if (n_s[s] != tiers) {
                    ordered_stacks_not_full++;
                }
                if (S_N.size() == 1 && n_s[s] != tiers) {
                    case_2a = true;
                    case_2b = true;
                } else if (S_N.size() == 1) {
                    case_2b = true;
                }
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

    public static int compute__IBF_0(int stacks, int tiers) {
        int IBF_0;
        boolean all_non_misoverlaid_full = false;
        for (int s: S_N) {
            if (n_s[s] == tiers) {
                all_non_misoverlaid_full = true;
            }
        }
        if (S_M.size() == stacks || all_non_misoverlaid_full) {
            IBF_0 = n_M + h_M + n_gx;
        } else {
            IBF_0 = n_M + n_gx;
        }
        return IBF_0;
    }

    public static int compute__LB_F(int[][][] initial_bay, int [][] s_info, int stacks, int tiers) {
        compute__groups(initial_bay, s_info, stacks);
        compute__n_g_s(initial_bay, s_info, stacks, tiers);
        compute__d_g__d_g_cum(initial_bay, s_info, stacks, tiers);
        compute__s_p_g__s_p_g_cum(initial_bay, s_info, stacks, tiers);
        compute__d_s_g_cum(initial_bay, stacks, tiers);

        return compute__n_m(stacks, tiers);
    }

    private static int compute__n_m(int stacks, int tiers) {
        if (S_M.size() == stacks) {
            n_bx = n_M + h_M;
        } else {
            n_bx = n_M;
        }

        //TODO: muss n_s_gx += 1 bei d_s_g_cum_max[0] / tiers, da Rest besteht?
        n_s_gx = Math.max(0, d_s_g_cum_max[0] / tiers);
        if (d_s_g_cum_max[0] % tiers > 0) {
            n_s_gx += 1;
        }
        //stacks werden nach n_g_s aufsteigend sortiert, um dann die n_g_s in den ersten n_s_gx stacks aufzusummieren
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
            //hier >=, damit bei Gleichstand, die größere Gruppe gewählt wird, da so n_gx richtig bestimmt wird
            if (d_s_g_cum[g] >= d_s_g_cum_max[0]) {
                d_s_g_cum_max[0] = d_s_g_cum[g];
                d_s_g_cum_max[1] = g;
            }
        }
    }

    private static void compute__s_p_g__s_p_g_cum(int[][][] initial_bay, int [][] s_info, int stacks, int tiers) {
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

    private static void compute__d_g__d_g_cum(int[][][] initial_bay, int [][] s_info, int stacks, int tiers) {
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

    private static void compute__groups(int[][][] initial_bay, int [][] s_info, int stacks) {
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

    private static void compute__n_g_s(int [][][] initial_bay, int [][] s_info, int stacks, int tiers) {

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

    public static void compute__params_LB_time(boolean multiple_bays, int stacks, int tiers) {
        if (multiple_bays) {
            t_min = 2 * PreMarshalling.speed_bays + 4 * PreMarshalling.speed_tiers;
        } else {
            t_min = 2 * PreMarshalling.speed_stacks + 4 * PreMarshalling.speed_tiers;
        }
        highest_tier_to_move_blocking_containers_to = new int [stacks][];
        for (int s = 0; s < stacks; s++) {
            if (S_M.contains(s)) {
                highest_tier_to_move_blocking_containers_to[s] = new int[n_M_s[s]];
                List<Integer> highest_tiers_to_move_blocking_containers_to = new ArrayList<>();
                for (int i = 0; i < n_M_s[s]; i++) {
                    for (int ss = 0; ss < stacks; ss++) {
                        if (s != ss) {
                            if (n_N_s[ss] + i <= tiers) {
                                highest_tiers_to_move_blocking_containers_to.add(n_N_s[ss] + i + 1);
                            }
                        }
                    }
                }
                for (int i = 0; i < n_M_s[s]; i++) {
                    highest_tier_to_move_blocking_containers_to[s][i] = Collections.max(highest_tiers_to_move_blocking_containers_to);
                    highest_tiers_to_move_blocking_containers_to.remove(Collections.max(highest_tiers_to_move_blocking_containers_to));
                }
            }
        }
    }

    public static double compute__LB_time(boolean multiple_bays, int stacks, int tiers) {
        //Zeit, die mindestens aufgebracht werden muss, um Blöcke, die bereits geordnet sind umzulagern
        lower_bound_time += (lower_bound_moves - n_M) * (t_min + 2 * PreMarshalling.speed_loading_unloading);
        for (int s = 0; s < stacks; s++) {
            for (int t = n_s[s]; t > n_N_s[s]; t--) {
                //Zeit, die mindestens aufgebracht werden muss, um ungeordneten Block umzulagern
                //TODO: theoretisch könnte von jedem ungeordneten Block aus, die Zeit zum nächst möglichen Umlagerplatz aufsummiert werden
                if (multiple_bays) {
                    lower_bound_time += 2 * PreMarshalling.speed_bays + (tiers - t + 1) * PreMarshalling.speed_tiers + (tiers - highest_tier_to_move_blocking_containers_to[s][n_s[s] - t] + 1) + 2 * PreMarshalling.speed_loading_unloading;
                } else {
                    lower_bound_time += 2 * PreMarshalling.speed_stacks + (tiers - t + 1) * PreMarshalling.speed_tiers + (tiers - highest_tier_to_move_blocking_containers_to[s][n_s[s] - t] + 1) + 2 * PreMarshalling.speed_loading_unloading;
                }
            }
        }
        return lower_bound_time / 3600;
    }
}
