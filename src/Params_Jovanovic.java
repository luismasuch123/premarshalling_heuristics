import java.util.*;
public class Params_Jovanovic {

    static int [][]  g_c_s; //number of containers above c in stack s
    static int [][] f_c_s; //containers above well located block c_a with larger or same due date value than c in destination stack s
    static int [][]nw_c_s; //well located containers that need to be relocated when c is moved to destination stack s
    static int [][] f_c_s_ext; //berücksichtigt zusätzlich zu f_c_s, dass Umlagerungen von bereits richtig platzierten Blöcken vermieden werden sollten
    static int [][] w_c_s; //Summe der Blöcke, die bei einer Umlagerung von c aus stack s in destination stack ss, aus s und ss heraus umgelagert werden müssen
    static int [] d_c; //d_c is the index of the stack that the lowest value of w_c_s for a block c and if tied the minimum distance to c

    public static void compute_params_Jovanovic(int[][][] copy, int stacks, int stacks_per_bay, int tiers, int containers, boolean consider_time) {
        Params.compute__c_info__s_info(copy, stacks, tiers);
        Params.compute__bay_info(copy, stacks);
        compute__g_c_s(copy, stacks, tiers, containers);
        compute__f_c_s__nw_c_s(copy, Params.c_info, stacks, tiers, containers);
        compute__f_c_s_ext(stacks, containers);
        compute__w_c_s(Params.c_info, stacks, containers);
        compute_d_c(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, containers, consider_time);
    }

    public static void compute__g_c_s(int [][][] copy, int stacks, int tiers, int containers) {
        g_c_s = new int[containers][stacks];
        for(int s = 0; s < stacks; s++) {
            int containers_above_c = 0;
            for (int t = tiers-1; t >= 0; t--) {
                if (copy[s][t][0] != 0) {
                    g_c_s[copy[s][t][0]-1][s] = containers_above_c;
                    containers_above_c++;
                }
            }
        }
    }

    public static void compute__w_c_s(int [][] c_info, int stacks, int containers) {
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

    public static int[] compute_fr_c_s(int [][][] copy, int [][] bay_info, int [][] c_info, int [][] s_info, int[] fr_c_s, int[] d_c, Object[] next_options) {
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

    public static void compute_d_c(int [][][] copy, int [][] c_info, int [][] s_info, int stacks, int stacks_per_bay, int tiers, int containers, boolean consider_time) {
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
                    double time_to_current_stack = Params.get_time(c_info[c][0], indice, c_info[c][1], s_info[indice][0] - f_c_s[c][indice], tiers, stacks_per_bay);
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

    public static void compute__f_c_s__nw_c_s(int [][][] copy, int [][] c_info, int stacks, int tiers, int containers) {
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
}
