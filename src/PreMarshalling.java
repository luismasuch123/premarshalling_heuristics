import java.io.FileNotFoundException;
import java.util.*;

public class PreMarshalling {

    static String [] method = {"Jovanovic", "Huang", "LB"};
    static boolean consider_time; //soll Zeit bei Stack- bzw. Blockauswahl berücksichtigt werden
    static boolean multiple_bays; //sollen alle bays gleichzeitig berücksichtigt oder nacheinander premarshalled werden
    static boolean print_info; //sollen informationen zu einzelnen Umlagerungen und dem aktuellen Stand der bay ausgegeben werden
    static boolean print_statistics; //sollen erhobene statistische Daten ausgegeben werden
    static boolean print_relocations; //sollen die endgültigen Umlagerungen ausgegeben werden

    static int [][][] current_bay; //aktueller Stand der bay

    static TreeSet<Relocation> relocations; //relocation_count, block, prev_stack, next_stack, prev_tier, next_tier
    static TreeSet<Relocation> relocations_on_hold; //Umlagerungen, die noch aufgehoben/rückgängig gemacht werden könnten

    static int step; //Algorithmus-Schritt
    static int relocation_count_current;
    static double time_relocations; //keine Leerfahrten berücksichtigt, ohne Lastaufnahme und -abgabe
    static double time_total; //ohne Lastaufnahme und -abgabe

    static boolean solution_found = true;

    //Jovanovic
    static String next_selection = "function h_c"; //"highest due date value"
    static String stack_selection = "The Lowest Position";//"The Lowest Position", "Lowest Priority Index", "MinMax"
    static String stack_filling = "None"; //"None", "Standard", "Safe", "Stop"

    //Huang
    static boolean correction; //soll die gefundene Lösung optimiert werden, indem überflüssige Umlagerungen, wie bei Jovanovic, gelöscht werden

    //LB
    static String lower_bound_method = "IBF_3"; //"LB_F", "IBF_0", "IBF_1", "IBF_2", "IBF_3", "IBF_4"

    //speed in sec/m
    static double speed_tiers = 15; //Dauer Bewegung zwischen Ebenen
    static double speed_stacks = 2.4; //Dauer Bewegung zwischen stacks
    static double speed_bays = 1.875; //Dauer Bewegung zwischen bays
    //speed in sec
    static double speed_loading_unloading = 20; //Dauer Lastaufnahme/-abgabe

    public static void main (String [] args) throws FileNotFoundException {
        String initial_bay_path = "/Users/luismasuchibanez/IdeaProjects/premarshalling_heuristics/data/Test/emm_s10_t4_p1_c0_16.bay";
        consider_time = true;
        multiple_bays = true;
        print_info = false;
        print_statistics = true;
        print_relocations = false;

        //Huang
        correction = true;
        Relocation_Huang.beta = 0.2; //scheint am besten zu sein wenn beta_h <= 1

            try {
                BayInstance instance = BayInstance.get_initial_bay(initial_bay_path, multiple_bays);
                for (String m: method) {
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
                final_bay = (int[][][]) premarshall_Jovanovic(initial_bay, stacks, stacks_per_bay, tiers, containers, Relocation_Jovanovic.order_relocations, Relocation_Jovanovic.same_stack_under, Relocation_Jovanovic.same_stack_below)[0];
                solution_found = (boolean) premarshall_Jovanovic(initial_bay, stacks, stacks_per_bay, tiers, containers, Relocation_Jovanovic.order_relocations, Relocation_Jovanovic.same_stack_under, Relocation_Jovanovic.same_stack_below)[1];
            } else if (method.equals("Huang")) {
                final_bay = (int[][][]) premarshall_Huang(initial_bay, stacks, stacks_per_bay, tiers, containers)[0];
                solution_found = (boolean) premarshall_Huang(initial_bay, stacks, stacks_per_bay, tiers, containers)[1];
            }

            time_relocations = (((double) Relocation.distance_relocations[0] * 2 * speed_tiers) + ((double) Relocation.distance_relocations[1] * 2 * speed_stacks) + ((double) Relocation.distance_relocations[2] * 2 * speed_bays)) / 3600;
            time_total = (((double) Relocation.distance_total[0] * 2 * speed_tiers) + ((double) Relocation.distance_total[1] * 2 * speed_stacks) + ((double) Relocation.distance_total[2] * 2 * speed_bays) + Relocation.relocations_count * 2 * speed_loading_unloading) / 3600;

            if (print_info) {
                System.out.println("Final bay: " + Arrays.deepToString(final_bay));
            }
            if (print_statistics) {
                System.out.println("Relocations: " + Relocation.relocations_count);
                System.out.println("Distance_relocations in blocks: " + Arrays.toString(Relocation.distance_relocations));
                System.out.println("Distance_total in blocks: " + Arrays.toString(Relocation.distance_total));
                System.out.println("Time_relocations in h: " + time_relocations);
                System.out.println("Time_total in h: " + time_total);
                if (Relocation_Jovanovic.deadlock_count > 0) {
                    System.out.println("Deadlocks: " + Relocation_Jovanovic.deadlock_count);
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
            Params_LB.lower_bound_moves = compute_LB_moves(initial_bay, stacks, tiers, containers);
            Params_LB.lower_bound_time = compute_LB_time(stacks, tiers);
            System.out.println("LB moves: " + Params_LB.lower_bound_moves);
            System.out.println("LB time in h: " + Params_LB.lower_bound_time);
        }
    }

    private static void reset_statistics() {
        step = 0;
        relocations = new TreeSet<>();
        relocation_count_current = 0;
        time_relocations = 0;
        time_total = 0;
        Relocation.next_block = 0;
        Relocation.prev_block = 0;
        Relocation.relocations_count = 0;
        Relocation.distance_relocations = new int [3];
        Relocation.distance_total = new int [3];
        Relocation_Jovanovic.deadlock_count = 0;
    }

    private static int compute_LB_moves(int [][][] initial_bay, int stacks, int tiers, int containers) {
        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];
        Params.compute__c_info__s_info(initial_bay, stacks, tiers);
        Params.check_sorted(stacks);

        Params_LB.compute__params_IBF(initial_bay, Params.s_info, stacks, tiers);

        int LB_F = Params_LB.compute__LB_F(initial_bay, Params.s_info, stacks, tiers);

        int IBF_0 = Params_LB.compute__IBF_0(stacks, tiers);

        int IBF_1 = Params_LB.compute__IBF_1(IBF_0, stacks, tiers);

        int IBF_2 = Params_LB.compute__IBF_2(LB_F, IBF_0, IBF_1, stacks, tiers);

        int IBF_3 = Params_LB.compute__IBF_3(initial_bay, LB_F, IBF_2, stacks, tiers);

        int IBF_4 = Params_LB.compute__IBF_4(initial_bay, LB_F, IBF_3, stacks);

        switch (lower_bound_method) {
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
        return 0;
    }

    public static double compute_LB_time(int stacks, int tiers) {
        Params_LB.compute__params_LB_time(multiple_bays, stacks, tiers);

        return Params_LB.compute__LB_time(multiple_bays, stacks, tiers);
    }

    private static Object[] premarshall_Huang(int[][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers) {
        current_bay = new int[stacks][tiers][3];
        current_bay = BayInstance.copy_bay(initial_bay, current_bay, stacks, tiers);

        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];

        Relocation.copy = new int[stacks][tiers][3];
        while(!Params.sorted && solution_found) {
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
            Relocation_Huang.complete_high_R_stacks(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
            Params.check_sorted(stacks);

            relocations.addAll(relocations_on_hold); //relocations hinzufügen, da sonst auch die bisherigen zulässigen relocations gelöscht werden, wenn bei complete the low R stacks relocations rückgängig gemacht werden

            //complete the low R stacks
            if (!Params.sorted) {
                Relocation_Huang.complete_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }
            Params.check_sorted(stacks);

            //deconstruct the low R stacks
            if (!Params.sorted) {
                Relocation_Huang.deconstruct_low_R_stacks(copy, current_bay, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }
            Params.check_sorted(stacks);

            if (!Params.sorted) {
                Relocation_Huang.move_W_to_empty_stack(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }

            //wenn alle R stacks voll sind und sonst nur W stacks existieren, dann kleinsten W stack zu empty stack machen
            //dieser Schritt fehlt im Paper bzw. kann es ohne diesen Schritt zu unendlichen Loops kommen, indem immer wieder dieselben Schritte durchgeführt werden
            if (relocation_count_current == Relocation.relocations_count) {
                Relocation_Huang.create_empty_stack(copy, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, multiple_bays, consider_time);
            }
            Params.check_sorted(stacks);

            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);
            relocations.addAll(relocations_on_hold);

            //wenn in einem Schritt keine Umlagerungen oder unzulässige Umlagerungen durchgeführt wurden, wird der Algorithmus abgebrochen
            if (relocation_count_current == Relocation.relocations_count || !solution_found) {
                if (print_info) {
                    System.out.println("No feasible relocation found or illegal move!");
                }
                solution_found = false;
            }
        }
        //Corrections bei Huang als Option (im Original nicht dabei), aber können nur zu Verbesserungen führen
        if (correction && solution_found) {
            correction();
        }

        return new Object [] {current_bay, solution_found};
    }

    public static Object[] premarshall_Jovanovic(int[][][] initial_bay, int stacks, int stacks_per_bay, int tiers, int containers, int [] order_relocations, boolean same_stack_under, int [] same_stack_below) {
        current_bay = new int[stacks][tiers][3];
        current_bay = BayInstance.copy_bay(initial_bay, current_bay, stacks, tiers);

        Params.s_info = new int [stacks][2];
        Params.c_info = new int[containers][4];

        Relocation.copy = new int[stacks][tiers][3]; //Kopie, damit current_bay nicht verändert wird, falls Schritt rückgängig gemacht werden muss nach look-ahead

        Relocation_Jovanovic.deadlock = false;
        Relocation_Jovanovic.deadlock_next = new int[containers];
        while(!Params.sorted && solution_found) {
            relocations_on_hold = new TreeSet<>();
            step++;
            if (print_info) {
                System.out.println("Step " + step);
                System.out.println("Current bay: " + Arrays.deepToString(current_bay));
            }

            Relocation.copy = BayInstance.copy_bay(current_bay, Relocation.copy, stacks, tiers);
            int [][][] copy = Relocation.copy;

            Params_Jovanovic.compute_params_Jovanovic(copy, stacks, stacks_per_bay, tiers, containers, consider_time);

            Relocation_Jovanovic.get_next_Jovanovic(copy, Relocation.next_block, Relocation.prev_block, Params.c_info, Params.s_info, Params_Jovanovic.d_c, Params_Jovanovic.w_c_s, Params_Jovanovic.f_c_s, next_selection, consider_time, containers, tiers, stacks_per_bay);
            int next = Relocation.next_block;
            if (print_info) {
                System.out.println("next: " + next + " from stack " + Params.c_info[next][0] + " to stack " + Params_Jovanovic.d_c[next]);
            }

            Relocation_Jovanovic.same_stack = false;
            Relocation_Jovanovic.same_stack_over = false;
            Relocation_Jovanovic.same_stack_under = false;
            Relocation_Jovanovic.next_to_stopover_stack_prevent_deadlock = false;

            Relocation_Jovanovic.compute__order_relocations__same_stack_below_Jovanovic(copy, Params.c_info, Params.s_info, Params_Jovanovic.d_c, Params_Jovanovic.g_c_s, Params_Jovanovic.f_c_s, next);
            if (print_info) {
                System.out.println("order_relocations: " + Arrays.toString(order_relocations));
                if (same_stack_under) {
                    System.out.println("same_stack_below: " + Arrays.toString(same_stack_below));
                }
            }

            //die Blöcke umlagern, die next blockieren (egal welcher stack) oder next's Platz im destination stack blockieren (wenn stack gleich destination stack)
            Relocation_Jovanovic.relocate_blocking_blocks_Jovanovic(copy, Relocation.next_block, Params.c_info, Params.s_info, Params_Jovanovic.d_c, stack_selection, consider_time, multiple_bays, stacks, stacks_per_bay, tiers);

            //next umlagern
            if (!Relocation_Jovanovic.deadlock) {
                Relocation_Jovanovic.deadlock_next = new int[containers];
                Relocation_Jovanovic.relocate_next_Jovanovic(copy, Relocation.next_block, Params.c_info, Params.s_info, Params_Jovanovic.d_c, stack_selection, consider_time, multiple_bays, stacks, tiers, stacks_per_bay);
            } else {
                Relocation_Jovanovic.deadlock_next[next] = next+1;
                if (print_info) {
                    System.out.println("DEADLOCK!");
                    System.out.println("deadlock_next: " + Arrays.toString(Relocation_Jovanovic.deadlock_next));
                }
                Relocation_Jovanovic.deadlock_count++;
                Relocation.copy = BayInstance.copy_bay(current_bay, copy, stacks, tiers);
                relocations_on_hold.clear();
                //TODO: Relocation.relocations_count = relocations.size();
            }

            //filling
            if (!Relocation_Jovanovic.deadlock) {
                Relocation_Jovanovic.stack_filling_Jovanovic(copy, Relocation.next_block, Relocation.prev_block, Params.c_info, Params.s_info, stacks, stacks_per_bay, tiers, stack_filling, multiple_bays, consider_time);
                relocations.addAll(relocations_on_hold);

            }

            current_bay = BayInstance.copy_bay(Relocation.copy, current_bay, stacks, tiers);

            Params.check_sorted(stacks);
            if (relocation_count_current == Relocation.relocations_count || !solution_found) {
                if (print_info) {
                    System.out.println("No feasible relocation found or illegal move!");
                }
                solution_found = false;
            }
        }
        correction();

        return new Object[] {current_bay, solution_found};
    }

    private static void correction() {
        Relocation last = new Relocation(0, 0, 0, 0, 0, 0);
        int number_relocations = relocations.size();
        //Sicherstellen, dass relocation_count mit relocations.size() übereinstimmt
        //TODO: Fehler finden
        int i = 1;
        for (Relocation r: relocations) {
            r.relocation_count = i;
            i++;
        }
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
