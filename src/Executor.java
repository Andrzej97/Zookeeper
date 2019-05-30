
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Executor
        implements Watcher, Runnable, DataMonitor.DataMonitorListener {

    DataMonitor dm;
    ZooKeeper zk;
    String exec[];
    Process child;

    public Executor(String hostPort,
                    String exec[]) throws KeeperException, IOException{
        this.exec = exec;
        zk = new ZooKeeper(hostPort, 3000, this);
        dm = new DataMonitor(zk, null, this);
        new Interface(zk, dm).run();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err
                    .println("USAGE: Executor hostAddress:hostPort programToExecute [args ...]");
            System.exit(2);
        }
        String hostPort = args[0];
        String exec[] = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);
        try {
            new Executor(hostPort, exec).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // implementation of the only one method of Watcher interface:
    public void process(WatchedEvent event) {
        dm.process(event);
    }

    // implementation of the only one method of Runnable interface:
    public void run() {
        try {
            synchronized (this) {
                while (!dm.dead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    // implementation of 4 methods of DataMonitor.DataMonitorListener interface:
    @Override
    public void createAppInstance() {
//        System.out.println("Executor.createAppInstance()");
        try {
            child = Runtime.getRuntime().exec(exec);
            new StreamWriter(child.getInputStream(), System.out);
            new StreamWriter(child.getErrorStream(), System.err);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int countChildren(String path) {
//        System.out.println("Executor.countChildren()");
        return countChildrenRecursive(path);
    }

    @Override
    public void deleteAppInstance() {
//        System.out.println("Executor.deleteAppInstance()");
        if (child != null) {
            System.out.println("Killing process");
            child.destroy();
            try {
                child.waitFor();
            } catch (InterruptedException e) {
            }
        }
        child = null;
    }

    @Override
    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    private int countChildrenRecursive(String path) {
//        System.out.println("Executor.countChildrenRecursive(). path:" + path);
        List<String> children;
        try {
            children = zk.getChildren(path, false);
        } catch (KeeperException e) {
            return 0;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return 0;
        }
        return children.size() + children.stream().mapToInt(path1 -> {
            return countChildrenRecursive(path + "/" + path1);
        }).sum();
    }


    // for running exec application
    static class StreamWriter extends Thread {
        OutputStream os;

        InputStream is;

        StreamWriter(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        public void run() {
            byte b[] = new byte[80];
            int rc;
            try {
                while ((rc = is.read(b)) > 0) {
                    os.write(b, 0, rc);
                }
            } catch (IOException e) {
            }

        }
    }
}

