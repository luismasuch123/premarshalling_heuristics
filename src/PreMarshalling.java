import java.io.FileNotFoundException;
import java.util.*;

public class PreMarshalling {

    static String [] method = {"Jovanovic", "Huang", "LB"};
    static boolean consider_time;
    static boolean multiple_bays;
    static boolean print_info;
    static boolean print_statistics;
    static boolean print_relocations;

    static int [][][] current_bay;

    static TreeSet<Relocation> relocations; //relocation_count, block, prev_stack, next_stack, prev_tier, next_tier
    static TreeSet<Relocation> relocations_on_hold;

    static int step;
    static int relocation_count_current;
    static double time_relocations; //keine Leerfahrten berücksichtigt, ohne Lastaufnahme und -abgabe
    static double time_total; //ohne Lastaufnahme und -abgabe

    static boolean solution_found = true;

    //Jovanovic
    static String next_selection = "function h_c"; //"highest due date value"
    static String stack_selection = "The Lowest Position";//"The Lowest Position", "Lowest Priority Index", "MinMax"
    static String stack_filling = "None"; //"None", "Standard", "Safe", "Stop"

    //Huang
    static boolean correction;

    //LB
    static int lower_bound_moves;
    static String lower_bound_method = "IBF_2"; //"LB_F", "IBF_0", "IBF_1", "IBF_2", "IBF_3", "IBF_4"
    //TODO: Aus allen LB's die größte auswählen

    public static void main (String [] args) throws FileNotFoundException {
        String initial_bay_path = "/Users/luismasuchibanez/IdeaProjects/premarshalling_heuristics/data/Test/LB_case4.bay";
        consider_time = true;
        multiple_bays = true;
        print_info = false;
        print_statistics = true;
        print_relocations = false;

        //Huang
        correction = true;
        Params.beta = 0.2; //scheint am besten zu sein wenn beta_h <= 1

            try {
                BayInstance instance = BayInstance.get_initial_bay(initial_bay_path, multiple_bays);
                for (String m: method) {
                    if (m.equals("LB")) {
                        reset_statistics();
                        System.out.println("\n" + m.toUpperCase() + "\n");
                        if (multiple_bays) {
                            int[][][] initial_bay = instance.initial_bay;
                            int stacks = instance.stacks;
                            int stacks_per_bay = instance.stacks_per_bay;
                            int tiers = instance.tiers;
                            int containers = instance.containers;

                            run_methods(instance, m, initial_bay, stacks, stacks_per_bay, tiers, containers);

                        } else {
                            int stacks = instance.stacks_per_bay;
                            int stacks_per_bay = instance.stacks_per_bay;
                            int tiers = instance.tiers;
                            for (int b = 0; b < instance.bays; b++) {
                                Relocation.current_bay = b;
                                step = 0;
                                int containers = instance.containers_per_bay[b];
                                int[][][] initial_bay = instance.initial_bays[b];

                                run_methods(instance, m, initial_bay, stacks, stacks_per_bay, tiers, containers);
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println(("File not found!"));
            }
        
    }

    private static void run_methods(BayInstance instance, String method, int [][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers) {
        //check if blocks are well located or not (1:well located, 0: not well located)
        Params.compute_if_well_located(initial_bay, stacks, tiers);
        if (print_info) {
            System.out.println("Initial bay: " + Arrays.deepToString(instance.initial_bay));
        }
        Params.check_sorted_pre(initial_bay, stacks, tiers);

        if (method.equals("Jovanovic") || method.equals("Huang")) {
            int[][][] final_bay = new int[stacks][tiers][3];
            if (method.equals("Jovanovic")) {
                final_bay = premarshall_Jovanovic(initial_bay, stacks, stacks_per_bay, tiers, containers, Relocation.order_relocations, Relocation.same_stack_under, Relocation.same_stack_below);
            } else if (method.equals("Huang")) {
                //TODO: Relocations und alles weitere nach Jovanovic zurücksetzen
                //TODO: Wenn System.exit() sollen andere methods trotzdem durchlaufen!
                final_bay = premarshall_Huang(initial_bay, stacks, stacks_per_bay, tiers, containers);
            }

            time_relocations = (((double) Relocation.distance_relocations[0] * 2 * 15) + ((double) Relocation.distance_relocations[1] * 2 * 2.4) + ((double) Relocation.distance_relocations[2] * 2 * 1.875)) / 3600;
            time_total = (((double) Relocation.distance_total[0] * 2 * 15) + ((double) Relocation.distance_total[1] * 2 * 2.4) + ((double) Relocation.distance_total[2] * 2 * 1.875) + Relocation.relocations_count * 2 * 20.0) / 3600;

            if (print_info) {
                System.out.println("Final bay: " + Arrays.deepToString(final_bay));
            }
            if (print_statistics) {
                System.out.println("Relocations: " + Relocation.relocations_count);
                System.out.println("Distance_relocations in blocks: " + Arrays.toString(Relocation.distance_relocations));
                System.out.println("Distance_total in blocks: " + Arrays.toString(Relocation.distance_total));
                System.out.println("Time_relocations in h: " + time_relocations);
                System.out.println("Time_total in h: " + time_total);
                if (Relocation.deadlock_count > 0) {
                    System.out.println("Deadlocks: " + Relocation.deadlock_count);
                }
                System.out.println("True number relocations: " + relocations.size());
                System.out.println("Solution found: " + solution_found);
            }
            if (print_relocations) {
                for (Relocation relocation : relocations) {
                    System.out.println(relocation.toString());
                }
            }
        } else if (method.equals("LB")) {
            lower_bound_moves = compute_LB(initial_bay, stacks, stacks_per_bay, tiers, containers);
            System.out.println("LB moves: " + lower_bound_moves);
        }
    }

    private static void reset_statistics() {
        step = 0;
        relocations = new TreeSet<>();
        relocation_count_current = 0;
        time_relocations = 0;
        time_total = 0;
        Relocation.relocations_count = 0;
        Relocation.distance_relocations = new int [3];
        Relocation.distance_total = new int [3];
    }

    private static int compute_LB(int [][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers) {
        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];
        Params.compute__c_info__s_info(initial_bay, stacks, tiers);
        Params.check_sorted(stacks);

        if(!Params.sorted) {
            return Params.compute_params_LB(initial_bay, stacks, stacks_per_bay, tiers, containers);
        } else {
            return 0;
        }
    }

    private static int[][][] premarshall_Huang(int[][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers) {
        current_bay = new int[stacks][tiers][3];
        current_bay = BayInstance.copy_bay(initial_bay, current_bay, stacks, tiers);

        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];

        Relocation.copy = new int[stacks][tiers][3];
        while(!Params.sorted) {
            relocations_on_hold = new TreeSet<>();
            step++;
            relocation_count_current = Relocation.relocations_count;
            if (print_info) {
                System.out.println("Step " + step);
                System.out.println("Current bay: " + Arrays.deepToString(current_bay));
            }

            Relocation.copy = BayInstance.copy_bay(current_bay, Relocation.copy, stacks, tiers);
            int [][][] copy = Relocation.copy;

            Params.compute__c_info__s_info(copy, stacks, tiers);

            //complete the high R stacks (Stacks, die geordnet sind und deren Höhe mindestens beta * h ist
            Params.complete_high_R_stacks(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
            //Relocation.copy = BayInstance.copy_bay(current_bay, Relocation.copy, stacks, tiers);
            Params.check_sorted(stacks);

            relocations.addAll(relocations_on_hold); //relocations hinzufügen, da sonst auch die bisherigen zulässigen relocations gelöscht werden, bei complete the low R stacks

            //complete the low R stacks
            if (!Params.sorted) {
                Params.complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }
            Params.check_sorted(stacks);

            //deconstruct the low R stacks
            //TODO: wenn im letzten step alle low_R stacks nicht erfolgreich deconstructed wurden, dann nicht zulassen, dass sie wieder deconstructed werden!
            if (!Params.sorted) {
                Params.deconstruct_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }

            if (!Params.sorted) {
                Params.move_W_to_empty_stack(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }

            //wenn alle R stacks voll sind und sonst nur W stacks existieren, dann kleinsten W stack zu empty stack machen
            if (relocation_count_current == Relocation.relocations_count) {
                Params.create_empty_stack(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }

            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
            relocations.addAll(relocations_on_hold);

            Params.check_sorted(stacks);
            if (relocation_count_current == Relocation.relocations_count) {
                System.out.println("No feasible relocation found!");
                System.exit(0);
            }
        }
        //Corrections bei Huang nur als Option bereitstellen (im Original nicht dabei)
        if (correction) {
            correction();
        }

        return current_bay;
    }

    public static int[][][] premarshall_Jovanovic(int[][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers, int [] order_relocations, boolean same_stack_under, int [] same_stack_below) {
        current_bay = new int[stacks][tiers][3];
        current_bay = BayInstance.copy_bay(initial_bay, current_bay, stacks, tiers);

        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];

        Relocation.copy = new int[stacks][tiers][3]; //Kopie, damit current_bay nicht verändert wird, falls Schritt rückgängig gemacht werden muss nach look-ahead

        Relocation.deadlock = false;
        Relocation.deadlock_next = new int[containers];
        while(!Params.sorted) {
            relocations_on_hold = new TreeSet<>();
            step++;
            if (print_info) {
                System.out.println("Step " + step);
                System.out.println("Current bay: " + Arrays.deepToString(current_bay));
            }

            Relocation.copy = BayInstance.copy_bay(current_bay, Relocation.copy, stacks, tiers);
            int [][][] copy = Relocation.copy;

            Params.compute_params_Jovanovic(copy, stacks, stacks_per_bay, tiers, containers, consider_time);

            Relocation.get_next_Jovanovic(Params.c_info, Params.s_info, Params.d_c, Params.w_c_s, Params.f_c_s, next_selection, consider_time, containers, tiers, stacks_per_bay);
            int next = Relocation.next_block;
            if (print_info) {
                System.out.println("next: " + next + " from stack " + Params.c_info[next][0] + " to stack " + Params.d_c[next]);
            }

            Relocation.same_stack = false;
            Relocation.same_stack_over = false;
            Relocation.same_stack_under = false;
            Relocation.next_to_stopover_stack_prevent_deadlock = false;

            Relocation.compute__order_relocations__same_stack_below_Jovanovic(Params.c_info, Params.s_info, Params.d_c, Params.g_c_s, Params.f_c_s, next);
            if (print_info) {
                System.out.println("order_relocations: " + Arrays.toString(order_relocations));
                if (same_stack_under) {
                    System.out.println("same_stack_below: " + Arrays.toString(same_stack_below));
                }
            }

            //die Blöcke umlagern, die next blockieren (egal welcher stack) oder next's Platz im destination stack blockieren (wenn stack gleich destination stack)
            Relocation.relocate_blocking_blocks_Jovanovic(Params.c_info, Params.s_info, Params.d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);

            //next umlagern
            if (!Relocation.deadlock) {
                Relocation.deadlock_next = new int[containers];
                Relocation.relocate_next_Jovanovic(Params.c_info, Params.s_info, Params.d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
            } else {
                Relocation.deadlock_next[next] = next+1;
                if (print_info) {
                    System.out.println("DEADLOCK!");
                    System.out.println("deadlock_next: " + Arrays.toString(Relocation.deadlock_next));
                }
                Relocation.deadlock_count++;
                Relocation.copy = BayInstance.copy_bay(current_bay, copy, stacks, tiers);
                relocations_on_hold.clear();
            }

            //filling
            if (!Relocation.deadlock) {
                Relocation.stack_filling_Jovanovic(Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, stack_filling, multiple_bays, consider_time);
                relocations.addAll(relocations_on_hold);
            }

            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);

            Params.check_sorted(stacks);
        }

        correction();

        return current_bay;
    }

    private static void correction() {
        Relocation last = new Relocation(0, 0, 0, 0, 0, 0);
        int number_relocations = relocations.size();
        int [] last_relocation_count = new int[number_relocations];
        boolean correction_needed = true;
        while(correction_needed) {
            correction_needed = false;
            Iterator<Relocation> it = relocations.iterator();
            while (it.hasNext() && !correction_needed) {
                Relocation next = it.next();
                //sicherstellen, dass nicht letzter Block nach correction mit erstem verglichen wird
                if (next.block == last.block && next.relocation_count != 1) {
                    correction_needed = true;
                    int relocations_count = next.relocation_count;
                    int block = next.block;
                    int prev_stack = last.prev_stack;
                    int next_stack = next.next_stack;
                    int prev_tier = last.prev_tier;
                    int next_tier = next.next_tier;
                    last_relocation_count[last.relocation_count-1] = 1;
                    it.remove();
                    if (prev_stack != next_stack) {
                        relocations.add(new Relocation(relocations_count, block, prev_stack, next_stack, prev_tier, next_tier));
                    }
                }
                last = next;
            }
            if (correction_needed) {
                Iterator<Relocation> itt = relocations.iterator();
                boolean correction_done = false;
                while (itt.hasNext() && correction_needed && !correction_done) {
                    if (last_relocation_count[itt.next().relocation_count-1] == 1) {
                        itt.remove();
                        last_relocation_count = new int [number_relocations];
                        correction_done = true;
                    }
                }
            }
        }
    }
}
