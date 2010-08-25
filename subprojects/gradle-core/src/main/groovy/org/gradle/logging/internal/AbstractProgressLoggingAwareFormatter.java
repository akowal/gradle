/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.logging.internal;

import org.gradle.api.UncheckedIOException;
import org.gradle.api.logging.LogLevel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import static org.gradle.logging.StyledTextOutput.Style.*;

public abstract class AbstractProgressLoggingAwareFormatter implements OutputEventListener {
    public static final String EOL = System.getProperty("line.separator");
    private final LinkedList<Operation> pendingOperations = new LinkedList<Operation>();
    private boolean debugOutput;

    public void onOutput(OutputEvent event) {
        try {
            if (event instanceof ProgressStartEvent) {
                ProgressStartEvent progressStartEvent = (ProgressStartEvent) event;
                Operation operation = new Operation();
                operation.description = progressStartEvent.getDescription();
                operation.status = "";
                pendingOperations.addFirst(operation);
                onStart(operation);
            } else if (event instanceof ProgressEvent) {
                assert !pendingOperations.isEmpty();
                ProgressEvent progressEvent = (ProgressEvent) event;
                Operation operation = pendingOperations.getFirst();
                operation.status = progressEvent.getStatus();
                onStatusChange(operation);
            } else if (event instanceof ProgressCompleteEvent) {
                assert !pendingOperations.isEmpty();
                ProgressCompleteEvent progressCompleteEvent = (ProgressCompleteEvent) event;
                Operation operation = pendingOperations.removeFirst();
                operation.status = progressCompleteEvent.getStatus();
                onComplete(operation);
            } else if (event instanceof LogLevelChangeEvent) {
                debugOutput = ((LogLevelChangeEvent) event).getNewLogLevel() == LogLevel.DEBUG;
            } else {
                RenderableOutputEvent renderableEvent = (RenderableOutputEvent) event;
                String message = doLayout(renderableEvent);
                if (renderableEvent.getLogLevel() == LogLevel.ERROR) {
                    onErrorMessage(message);
                } else {
                    onInfoMessage(message);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String doLayout(RenderableOutputEvent event) {
        OutputEventTextOutput writer = new StringWriterBackedOutputEventTextOutput();
        writer.style(Normal);
        if (debugOutput) {
            writer.text(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(event.getTimestamp())));
            writer.text(" [");
            writer.text(event.getLogLevel());
            writer.text("] [");
            writer.text(event.getCategory());
            writer.text("] ");
        }
        event.render(writer);
        writer.style(Normal);
        return writer.toString();
    }

    protected abstract void onStart(Operation operation) throws IOException;

    protected abstract void onStatusChange(Operation operation) throws IOException;

    protected abstract void onComplete(Operation operation) throws IOException;

    protected abstract void onInfoMessage(String message) throws IOException;

    protected abstract void onErrorMessage(String message) throws IOException;

    protected class Operation {
        private String description;
        private String status;

        public String getDescription() {
            return description;
        }

        public String getStatus() {
            return status;
        }
    }
}
