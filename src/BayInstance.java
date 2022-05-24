import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
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

    public static int[][][] copy_bay(int[][][] initial_bay, int[][][] current_bay, int stacks, int tiers) {
        for(int s = 0; s < stacks; s++) {
            for (int t = 0; t < tiers; t++) {
                for (int i = 0; i < 3; i++) {
                    current_bay[s][t][i] = initial_bay[s][t][i];
                }
            }
        }
        return current_bay;
    }

    public static BayInstance get_initial_bay(String initial_bay_path, boolean multiple_bays) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(initial_bay_path));

        if (multiple_bays) {
            scanner.nextLine();
            scanner.nextLine();
            scanner.nextLine();
            scanner.next();
            int bays = scanner.nextInt();
            System.out.println("Bays: " + bays);
            scanner.next();
            int tiers = scanner.nextInt();
            System.out.println("Tiers: " + tiers);
            scanner.next();
            int stacks = scanner.nextInt();
            int stacks_per_bay = stacks / bays;
            System.out.println("Stacks: " + stacks);
            scanner.next();
            int containers = scanner.nextInt();
            System.out.println("Containers: " + containers);
            scanner.nextLine();

            int [][][] initial_bay = new int[stacks][tiers][3]; //3-te Dimension: Nummer, Prio, well-located
            int number_container = 1;
            for (int s = 0; s < stacks; s++) {
                String str = scanner.nextLine();
                str = str.replaceAll("[^-?0-9]+", " ");
                String[] help = (str.trim().split(" "));
                for (int t = 0; t < help.length - 1; t++) {
                    //priorities werden um jeweils 1 erhöht, damit leere felder den eintrag 0 erhalten können
                    initial_bay[s][t][0] = number_container;
                    initial_bay[s][t][1] = Integer.parseInt(help[t + 1]) + 1;
                    number_container++;
                }
            }
            return new BayInstance(bays, stacks, stacks_per_bay, tiers, containers, initial_bay);
        } else {
            scanner.nextLine();
            scanner.nextLine();
            scanner.nextLine();
            scanner.next();
            int bays = scanner.nextInt();
            System.out.println("Bays: " + bays);
            scanner.next();
            int tiers = scanner.nextInt();
            System.out.println("Tiers: " + tiers);
            scanner.next();
            int stacks = scanner.nextInt();
            int stacks_per_bay = stacks / bays;
            System.out.println("Stacks: " + stacks);
            scanner.next();
            int containers = scanner.nextInt();
            System.out.println("Containers: " + containers);
            scanner.nextLine();

            int [][][][] initial_bays = new int[bays][][][]; //3-te Dimension: Nummer, Prio, well-located
            int [] containers_per_bay = new int[bays];
            for (int b= 0; b < bays; b++) {
                int number_container = 1;
                int [][][] initial_bay = new int[stacks/bays][tiers][3]; //3-te Dimension: Nummer, Prio, well-located
                for (int s = 0; s < stacks/bays; s++) {
                    String str = scanner.nextLine();
                    str = str.replaceAll("[^-?0-9]+", " ");
                    String[] help = (str.trim().split(" "));
                    for (int t = 0; t < help.length - 1; t++) {
                        //priorities werden um jeweils 1 erhöht, damit leere felder den eintrag 0 erhalten können
                        initial_bay[s][t][0] = number_container;
                        initial_bay[s][t][1] = Integer.parseInt(help[t + 1]) + 1;
                        number_container++;
                    }
                }
                initial_bays[b] = initial_bay;
                containers_per_bay[b] = number_container-1;
            }
            return new BayInstance(bays, stacks_per_bay, tiers, containers_per_bay, initial_bays);
        }
    }

}
