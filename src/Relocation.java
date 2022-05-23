public class Relocation implements Comparable<Relocation> {
    int relocation_count;
    int block;
    int prev_stack;
    int next_stack;
    int prev_tier;
    int next_tier;

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
}
