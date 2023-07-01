package org.quangdao.tools;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.*;

@Log4j2
public class StateMachine {
    @Log4j2
    public static abstract class State {
        @Getter @Setter
        private String state = "CREATE";
        private CompletableFuture completableFuture = null;

        protected abstract State next() throws Exception;
        public State proceed() {
            try {
                return next();
            } catch (Throwable ex) {
                return new ErrorState(this, ex);
            }
        }

        protected abstract boolean isLoaded();
        public void startLoad() {
            state = "LOAD";
            completableFuture = CompletableFuture.runAsync(()->{
                if(this.isLoaded())
                    state = "READY";
                else state = "CREATE";
            });
        }

        public void stop() {
            completableFuture.cancel(true);
        }
    }

    @Log4j2
    public static class ErrorState extends State {
        @Getter
        private final Throwable exception;
        public ErrorState(State prev, Throwable exception) {
            this.exception = exception;
            log.error(prev);
            log.error(exception.getMessage(), exception);
        }

        @Override
        public State next() {
            return null;
        }

        @Override
        public boolean isLoaded() {
            return true;
        }
    }
}
