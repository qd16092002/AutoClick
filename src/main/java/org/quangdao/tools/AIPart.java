package org.quangdao.tools;

import lombok.Getter;
import org.quangdao.ui.ImageStore;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AIPart {
    private final AI.ImgInfo email, pass, failed, cap, valid, submit, reload;
    private final Rectangle part;
    @Getter
    private final String name;
    private final ScreenUtils screenUtils;
    private final AI parrent;

    private final ConcurrentHashMap<String, Point> cache;
    public AIPart(Map<String, ImageStore> imgStores, String name, ScreenUtils screenUtils, ConcurrentHashMap<String, Point> cache, AI parrent, Rectangle part) {
        this.part = part;
        this.parrent = parrent;
        this.screenUtils = screenUtils;
        this.name = name;
        this.cache = cache;
        email = AI.ImgInfo.gen(imgStores.get("email"));
        pass = AI.ImgInfo.gen(imgStores.get("pass"));
        failed = AI.ImgInfo.gen(imgStores.get("failed"));
        cap = AI.ImgInfo.gen(imgStores.get("cap"));
        valid = AI.ImgInfo.gen(imgStores.get("valid"));
        submit = AI.ImgInfo.gen(imgStores.get("submit"));
        reload = AI.ImgInfo.gen(imgStores.get("reload"));
    }

    private StateMachine.State state = new BeginLogin();

    public StateMachine.State getState() {
        return state;
    }

    public void stop() {
        state.stop();
    }

    public void nextState() {
        if(state == null) return;
        state = state.proceed();
    }

    private boolean finish = false;
    public boolean getFinish() {
        return finish;
    }
    public void setFinish(boolean t) {
        finish = t;
    }

    private class BeginLogin extends StateMachine.State {
        private Point emailPt = null, passPt = null, btnPt = null;

        @Override
        protected boolean isLoaded() {
                Point lastEmail = cache.get(name + "email"), lastPass = cache.get(name + "pass"), lastBtn = cache.get(name+"btn");
                    emailPt = screenUtils.find(email.img(), lastEmail, part);
                    if(emailPt == null) return false;
                    cache.put(name+"email", emailPt);
                    passPt = screenUtils.find(pass.img(), lastPass, part);
                    if(passPt == null) return false;
                    cache.put(name+"pass", passPt);
                    btnPt = screenUtils.find(submit.img(), lastBtn, part);
                    if(btnPt == null) return false;
                    cache.put(name+"btn", btnPt);
                    return true;
        }

        @Override
        protected StateMachine.State next() throws Exception {
            MailPass mailPass = parrent.getMailPass();
            while(emailPt != null)emailPt = screenUtils.fillText(email.img(), email.pt(), mailPass.mail(), emailPt, part);
            while(passPt != null)passPt = screenUtils.fillText(pass.img(), pass.pt(), mailPass.pass(), passPt, part);
            while(btnPt != null)btnPt = screenUtils.click(submit.img(), submit.pt(), btnPt, part);

            return new DecideState(mailPass);
        }
    }

    public class ReloadState extends StateMachine.State {
        Point reloadPt = null;
        @Override
        protected StateMachine.State next() throws Exception {
            Point pt = screenUtils.click(reload.img(), reload.pt(), reloadPt, part);
            if(pt == null) return null;
            cache.put(name+"reload", pt);
            return new BeginLogin();
        }

        @Override
        protected boolean isLoaded() {
            Point reloadCache = cache.get(name + "reload");
            reloadPt = screenUtils.find(reload.img(), reloadCache, part);
            return reloadPt != null;
        }
    }

    public class DecideState extends StateMachine.State {
        private final MailPass mailPass;
        public DecideState(MailPass mailPass) {
            this.mailPass = mailPass;
        }
        private Point validPt, failedPt, capPt;

        @Override
        protected StateMachine.State next() throws Exception {
            if(validPt!=null) {
                parrent.ack(mailPass);
                return new ReloadState();
            }
            if(capPt!=null) {
                return null;
            }
            return new ReloadState();
        }

        @Override
        protected boolean isLoaded() {
                Point lastValid = cache.get(name + "valid"), lastFail = cache.get(name + "fail"), lastCap = cache.get(name + "cap");
                    validPt = screenUtils.find(valid.img(), lastValid, part);
                    failedPt = screenUtils.find(failed.img(), lastFail, part);
                    capPt = screenUtils.find(cap.img(), lastCap, part);

                    if (validPt != null) cache.put(name + "email", validPt);
                    if (failedPt != null) cache.put(name + "pass", failedPt);
                    if (capPt != null) cache.put(name + "btn", capPt);

                    if (validPt != null || failedPt != null || capPt != null) {
                        return true;
                    }
                return false;
        }

    }
}



