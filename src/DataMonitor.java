
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.*;

public class DataMonitor implements Watcher, StatCallback {

    ZooKeeper zk;
    Watcher newNodeWatcher;
    Watcher chainedWatcher;
    boolean dead;
    DataMonitorListener listener;

    private Set<String> knownNodes = new HashSet<>();
    private int previousChildrenNumber = 0;

    public DataMonitor(ZooKeeper zk, Watcher chainedWatcher,
                       DataMonitorListener listener) {

        newNodeWatcher = event -> {
            System.out.println("DataMonitor: new event:\n\tpath:\t"
                    + event.getPath() + "\n\ttype:\t" + event.getType());
            applyNodesForNewNodeWatcher("/");
            Set<String> nodesToStalk = null;
            try {
                nodesToStalk = findNodesToProcess("/");

                HashSet<String> newNodes = new HashSet<>(nodesToStalk);
                newNodes.removeAll(knownNodes);
                newNodes.forEach(node -> {
                    if(node.equals("/z"))
                        listener.createAppInstance();
                });

                knownNodes = nodesToStalk;
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
//        System.out.println("DataMonitor constructor: after lambda");
        this.zk = zk;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        applyNodesForNewNodeWatcher("/");

        try {
            findNodesToProcess("/");
            knownNodes = findAllNodes("/");
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        previousChildrenNumber = listener.countChildren("/z");
    }

    void applyNodesForNewNodeWatcher(String patch) {
        try {
            zk.getChildren(patch, newNodeWatcher).forEach(c -> {
                applyNodesForNewNodeWatcher((patch.equals("/") ? "/" : patch + "/") + c);
            });

            zk.exists(patch, newNodeWatcher);

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void findNodesAndUpdateStuff() {
        try {
            Set<String> nodesToStalk = findNodesToProcess("/");
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Set<String> findNodesToProcess(String patch) throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(patch, false);

        Set<String> res = new HashSet<>();
        children.forEach(child -> {
            try {
                res.addAll(findNodesToProcess((patch.equals("/") ? "/" : patch + "/") + child));
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        if (patch.startsWith("/z")) {
            res.add(patch);
            try {
                zk.exists(patch, true);
                zk.getChildren(patch, true);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private Set<String> findAllNodes(String patch) throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(patch, false);

        Set<String> res = new HashSet<>();
        children.forEach(child -> {
            try {
                res.addAll(findNodesToProcess((patch.equals("/") ? "/" : patch + "/") + child));
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        res.add(patch);
        return res;
    }

    public interface DataMonitorListener {

        void createAppInstance();

        int countChildren(String path);

        void deleteAppInstance();

        void closing(int rc);
    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        System.out.println("DataMonitor.process(). \n\tpath:\t" +
                path + "\n\ttype:\t" + event.getType());
        if (event.getType() == Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else {
//            findNodesAndUpdateStuff();
            switch (event.getType()) {
                case NodeChildrenChanged:
                    try {
                        int childrenCount = listener.countChildren("/z");
                        if (childrenCount > previousChildrenNumber)
                            System.out.println(path + " has " + childrenCount + " children");
                        previousChildrenNumber = childrenCount;
                    } catch (Exception e) {
                        System.out.println("Children counting failed");
                        e.printStackTrace();
                    }
                    break;
                case NodeCreated:
                    //    System.out.println(path + " CREATED");
                    //     listener.createAppInstance();
                    break;
                case NodeDeleted:
                    //   System.out.println(path + " DEL");
                        if(path.equals("/z"))
                            listener.deleteAppInstance();
                    break;
                default:
                    break;
//                }
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case Code.Ok:
                exists = true;
                break;
            case Code.NoNode:
                exists = false;
                break;
            case Code.SessionExpired:
            case Code.NoAuth:
                dead = true;
                listener.closing(rc);
                return;
            default:
        }
    }
}
