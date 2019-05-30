import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;


public class Main implements Runnable {

    private final ZooKeeper zk;
    private final DataMonitor dm;

    public Main(ZooKeeper zk, DataMonitor dm) {
        this.zk = zk;
        this.dm = dm;
    }

    @Override
    public void run(){
        try {
            while (!dm.dead) {
                BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
                String menu = "Type:\n"
                        + "\ttree\t-\tto show the whole structure of the /z subtree\n";
                System.out.print(menu);
                String choice = buffer.readLine();
                if (dm.dead) break;
                switch (choice) {
                    case "tree":
                        System.out.println("Tree for znode /z:");
                        tryPrintTree();
                        break;
                    default:
                        System.out.println("You have chosen unavailable option. Try again!");
                }
            }
        } catch (Exception e) {
        }
    }

    private void tryPrintTree()throws KeeperException, InterruptedException{
        List<String> children = zk.getChildren("/", false);

        if(!children.contains("z")){
            System.out.println("\tCannot print tree. The znode /z does not exist.");
        }else{
            printSubtree("/z", 1);
        }
    }

    private void printSubtree(String patch, int tabs) {
        printNode(patch, tabs);

        try {
            List<String> children = zk.getChildren(patch, false);
            children.forEach(child -> {
                String childPath = (patch.equals("/") ? "/" : patch + "/") + child;
                printSubtree(childPath, tabs + 1);
            });
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printNode(String node, int tabs){
        for(int i = 0; i < tabs; i++)
            System.out.print("\t");
        System.out.println(node);
    }
}
