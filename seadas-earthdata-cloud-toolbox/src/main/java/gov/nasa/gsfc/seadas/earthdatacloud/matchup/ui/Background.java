package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Work off the event thread, with the result or the error back on it. */
final class Background {

    @FunctionalInterface
    interface Work<T> {
        T run() throws Exception;
    }

    private Background() {
    }

    /** Runs {@code work}; {@code done} gets its result, or an error is shown under {@code title}. */
    static <T> SwingWorker<T, Void> run(Component parent, String title, Work<T> work, Consumer<T> done,
                                         Runnable always) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    done.accept(get());
                } catch (CancellationException ignored) {
                    // Stopped by the user.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(parent, title, e.getCause());
                } finally {
                    if (always != null) {
                        always.run();
                    }
                }
            }
        };
        worker.execute();
        return worker;
    }

    static void showError(Component parent, String title, Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : error.toString();
        JOptionPane.showMessageDialog(parent, Text.wrap(message), title, JOptionPane.ERROR_MESSAGE);
    }
}
