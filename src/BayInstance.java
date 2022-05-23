import java.util.TreeSet;

public class BayInstance {

    public int bays;
    public int stacks;
    public int stacks_per_bay;
    public int tiers;
    public int containers;
    public int [] containers_per_bay;

    public int [][][][] initial_bays;
    public int [][][] initial_bay;

    BayInstance (int bays, int stacks_per_bay, int tiers, int [] containers_per_bay, int [][][][] initial_bays) {
        this.bays = bays;
        this.stacks_per_bay = stacks_per_bay;
        this.tiers = tiers;
        this.containers_per_bay = containers_per_bay;
        this.initial_bays = initial_bays;
    }

    BayInstance (int bays, int stacks, int stacks_per_bay, int tiers, int containers, int[][][] initial_bay) {
        this.bays = bays;
        this.stacks = stacks;
        this.stacks_per_bay = stacks_per_bay;
        this.tiers = tiers;
        this.containers = containers;
        this.initial_bay = initial_bay;
    }

}
