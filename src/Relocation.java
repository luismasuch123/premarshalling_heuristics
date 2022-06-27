import java.util.Arrays;

public class Relocation implements Comparable<Relocation> {
    int relocation_count;
    int block;
    int prev_stack;
    int next_stack;
    int prev_tier;
    int next_tier;

    static int [][][] copy;
    static int current_bay;

    static int next_block;
    static int prev_block; //letzter Block der umgelagert wurde

    static int relocations_count;
    static int [] distance_relocations; //tiers, stacks, bays in blocks
    static int [] distance_total;

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

    public static void relocate(int [][] c_info, int [][] s_info, int block, int prio, int next_stack, int tiers, int stacks_per_bay, boolean multiple_bays) {
        //distance von letztem Block zum nächsten/aktuellen
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
        if (PreMarshalling.print_info) {
            System.out.println("Block " + block + " from stack " + prev_stack + " to stack " + next_stack);
        }
        if (prev_tier != tiers-1 && copy[prev_stack][prev_tier+1][0] != 0) {
            if (PreMarshalling.print_info) {
                System.out.println("Illegal move!");
            }
            PreMarshalling.solution_found = false;
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
        //distance of the relocation
        distance_relocations[0] += (tiers - prev_tier) + (tiers - next_tier);
        distance_relocations[1] += Math.abs(next_stack % stacks_per_bay - prev_stack % stacks_per_bay);
        distance_relocations[2] += Math.abs(next_stack /stacks_per_bay - prev_stack/ stacks_per_bay);
        distance_total[0] += (tiers - prev_tier) + (tiers - next_tier);
        distance_total[1] += Math.abs(next_stack % stacks_per_bay - prev_stack % stacks_per_bay);
        distance_total[2] += Math.abs(next_stack /stacks_per_bay - prev_stack/ stacks_per_bay);

        if (multiple_bays) {
            PreMarshalling.relocations_on_hold.add(new Relocation(relocations_count, block, prev_stack, next_stack, prev_tier, next_tier));
        } else {
            PreMarshalling.relocations_on_hold.add(new Relocation(relocations_count, block, prev_stack + current_bay * stacks_per_bay, next_stack + current_bay * stacks_per_bay, prev_tier, next_tier));
        }
        if (PreMarshalling.print_info) {
            System.out.println("current_bay: " + Arrays.deepToString(copy));
        }
    }
}
