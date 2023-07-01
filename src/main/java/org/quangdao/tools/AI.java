package org.quangdao.tools;

import lombok.extern.log4j.Log4j2;
import org.quangdao.ui.ImageStore;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

@Log4j2
public class AI {
    public MailPass getMailPass() {
        synchronized (q) {
            try {
                if (q.size() == 0) {
                    q.addAll(fileIO.getEmail(100));
                    offset += q.size();
                }
                return q.poll();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void ack(MailPass mailPass) {
        fileIO.addEmail(mailPass);
    }

    record ImgInfo(SubImage img, Point pt) {
        static ImgInfo gen(ImageStore store) {
            return new ImgInfo(new SubImage(store.getImage(), store.getRect()), new Point(store.getX()-store.getRect().x,store.getY()-store.getRect().y));
        }
    }

    private final ConcurrentHashMap<String, Point> cache = new ConcurrentHashMap<>();
    private final FileIO fileIO;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(4);
    private final ScreenUtils screenUtils;
    private final Queue<MailPass> q = new ArrayDeque<>();
    private final List<AIPart> aiParts = new ArrayList<>();
    private Thread thread = null;

    private int offset = 0;

    public AI(Map<String,String> prop, List<Map<String, ImageStore>> imgStoreList) throws Exception {
        screenUtils = new ScreenUtils(Integer.parseInt(prop.get("exec.cache.box")));
        fileIO = new FileIO(prop.get("io.input"), prop.get("io.output"), prop.get("io.dump"));

        int index=0;
        for(var i : imgStoreList)
            aiParts.add(new AIPart(i, "AI-"+(index++), screenUtils, cache, this, i.get("scr").getRect()));

    }

    public synchronized void stop() {
        if(thread == null) return;
        thread.interrupt();
        while(true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException ignore){}
        }
        thread = null;
    }

    public synchronized void run() {
        if(thread != null) return;
        thread = new Thread(()->{
        int done = 0;
        while(done != aiParts.size() && !Thread.interrupted()) {
            for (var i : aiParts) {
                if(i.getState()==null) continue;
                if(i.getState().getState().equals("CREATE")) {
                    i.getState().startLoad();
                }
                else if(i.getState().getState().equals("READY")) {
                    i.nextState();
                    if(i.getState()==null) ++done;
                }

            }
        }
        });
        thread.start();
    }
}
