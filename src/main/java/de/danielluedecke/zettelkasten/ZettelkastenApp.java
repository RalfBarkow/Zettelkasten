/*
 * Zettelkasten - nach Luhmann
 * Copyright (C) 2001-2015 by Daniel Lüdecke (http://www.danielluedecke.de)
 * 
 * Homepage: http://zettelkasten.danielluedecke.de
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Dieses Programm ist freie Software. Sie können es unter den Bedingungen der GNU
 * General Public License, wie von der Free Software Foundation veröffentlicht, weitergeben
 * und/oder modifizieren, entweder gemäß Version 3 der Lizenz oder (wenn Sie möchten)
 * jeder späteren Version.
 * 
 * Die Veröffentlichung dieses Programms erfolgt in der Hoffnung, daß es Ihnen von Nutzen sein 
 * wird, aber OHNE IRGENDEINE GARANTIE, sogar ohne die implizite Garantie der MARKTREIFE oder 
 * der VERWENDBARKEIT FÜR EINEN BESTIMMTEN ZWECK. Details finden Sie in der 
 * GNU General Public License.
 * 
 * Sie sollten ein Exemplar der GNU General Public License zusammen mit diesem Programm 
 * erhalten haben. Falls nicht, siehe <http://www.gnu.org/licenses/>.
 */
package de.danielluedecke.zettelkasten;

import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import de.danielluedecke.zettelkasten.database.AutoKorrektur;
import de.danielluedecke.zettelkasten.database.StenoData;
import de.danielluedecke.zettelkasten.database.Synonyms;
import de.danielluedecke.zettelkasten.database.TasksData;
import de.danielluedecke.zettelkasten.settings.AcceleratorKeys;
import de.danielluedecke.zettelkasten.settings.Settings;
import de.danielluedecke.zettelkasten.util.Constants;
import de.danielluedecke.zettelkasten.util.FileOperationsUtil;

/**
 * The main class of the application.
 */
public class ZettelkastenApp extends SingleFrameApplication {

    private ByteArrayOutputStream inMemorySessionLog = new ByteArrayOutputStream(2000);
    private Settings settings;
    private AcceleratorKeys accKeys;
    private AutoKorrektur autoKorrekt;
    private Synonyms synonyms;
    private StenoData steno;
    private TasksData taskData;
    String[] params;

    private FileHandler createFileLogHandler() {
        try {
            // Create logging file handler that will split the log into up to 3 files with a
            // file size limit of 100Kb. It won't append to existing files, so each session
            // starts a separate file.
            FileHandler fh = new FileHandler(FileOperationsUtil.getZettelkastenHomeDir() + "zknerror%g.log", 102400, 3,
                    false);
            fh.setFormatter(new SimpleFormatter());
            return fh;
        } catch (IOException | SecurityException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return null;
        }
    }

    private void initLocale(Settings settings) {
        String languageFromSettings = settings.getLanguage();

        // Init supported locales.
        String englishCountryCode = new Locale("en", "", "").getLanguage();
        String germanCountryCode = new Locale("de", "", "").getLanguage();
        String spanishCountryCode = new Locale("es", "", "").getLanguage();
        String portugueseCountryCode = new Locale("pt", "", "").getLanguage();

        // Defaults to English.
        Locale newLocale = new Locale("en", "GB");

        if (languageFromSettings.equals(englishCountryCode)) {
            newLocale = new Locale("en", "GB");
        }
        if (languageFromSettings.equals(germanCountryCode)) {
            newLocale = new Locale("de", "DE");
        }
        if (languageFromSettings.equals(spanishCountryCode)) {
            newLocale = new Locale("es", "ES");
        }
        if (languageFromSettings.equals(portugueseCountryCode)) {
            newLocale = new Locale("pt", "BR");
        }

        Locale.setDefault(newLocale);
    }
    
    private void logSystemProperties(String[] args) {
        Properties props = System.getProperties();
        Set<Object> keys = props.keySet();
        for (Object key : keys) {
            String keyStr = (String) key;
            String value = props.getProperty(keyStr);
            Constants.zknlogger.log(Level.INFO, "System property: {0}={1}", new Object[]{keyStr, value});
        }
    }

    private void updateSettingsWithCommandLineParams(String[] params) {
        //logSystemProperties(params);

        for (String param : params) {
            processCommandLineParameter(param);
        }
    }

    private void processCommandLineParameter(String param) {
        Constants.zknlogger.log(Level.INFO, "Processing command line parameter: {0}", param);

        if (param.toLowerCase().endsWith(Constants.ZKN_FILEEXTENSION)) {
            processAsDataFile(param);
        } else {
            try {
                processAsInitialEntryNumber(param);
            } catch (NumberFormatException ex) {
                Constants.zknlogger.log(Level.WARNING, "Invalid command line parameter: {0}. Skipping.", param);
            }
        }
    }

    private void processAsDataFile(String param) {
        File file = new File(param);
        if (file.exists()) {
            Constants.zknlogger.log(Level.INFO, 
                    "Setting data file to {0} from the command line arguments.", 
                    file.toString());
            settings.setMainDataFile(file);
        } else {
            Constants.zknlogger.log(Level.WARNING, "Data file does not exist: '{0}'. Skipping.", param);
        }
    }

    private void processAsInitialEntryNumber(String param) {
        int initialZettellNr = Integer.parseInt(param);
        if (initialZettellNr > 0) {
            settings.setInitialParamZettel(initialZettellNr);
            Constants.zknlogger.log(Level.INFO,
                    "Setting initial entry number to {0} from the command line arguments.",
                    initialZettellNr); // Passing initialZettellNr as an argument
        } else {
            Constants.zknlogger.log(Level.WARNING, "Initial entry number is not positive: {0}. Skipping.", initialZettellNr);
        }
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        configureLogger(Constants.zknlogger);
        initializeTaskData();
        initializeSettings();
        initializeLocale();
        showMainWindow();
    }

    public Logger getLogger() {
        return Constants.zknlogger;
    }

    public TasksData getTaskData() {
        return taskData;
    }

    public Settings getSettings() {
        return settings;
    }

    public Locale getLocale() {
        return Locale.getDefault();
    }

    /**
     * Check if the main window is visible.
     *
     * @return true if the main window is visible, false otherwise.
     */
    public boolean isMainWindowVisible() {
        Frame mainFrame = getMainFrame();
        return mainFrame != null && mainFrame.isVisible();
    }

    void configureLogger(Logger logger) {
        logger.setLevel(Level.ALL);
        StreamHandler sHandler = new StreamHandler(inMemorySessionLog, new SimpleFormatter());
        logger.addHandler(sHandler);
        FileHandler fh = createFileLogHandler();
        if (fh != null) {
            logger.addHandler(fh);
        }
    }

    void initializeTaskData() {
        taskData = new TasksData();
    }

    void initializeSettings() {
        settings = new Settings();
        updateSettingsWithCommandLineParams(params);
    }

    void initializeLocale() {
        initLocale(settings);
    }

    void showMainWindow() {
        try {
            logStartingMainWindow();
            validateSettings();
            createMainWindow();
        } catch (IllegalArgumentException e) {
            handleMainWindowException(e);
        }
    }
    
    void logStartingMainWindow() {
        Constants.zknlogger.log(Level.INFO, "Starting Main Window.");
    }
    
    /**
     * @see Settings#resetSettings()
     */
    void validateSettings() {
        //TODO use default settings
        if (settings == null) {
            Settings settings = new Settings();
            settings.initDefaultSettingsIfMissing();
        }
    }

    void createMainWindow() {
        try {
            show(new ZettelkastenView(this, settings, taskData));
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | InstantiationException | IllegalAccessException | IOException e) {
            handleMainWindowException(e);
        }
    }

    private void handleMainWindowException(Exception e) {
        e.printStackTrace();
    }

    /**
     * A convenient static getter for the application instance.
     *
     * @return the instance of ZettelkastenApp
     */
    public static ZettelkastenApp getApplication() {
        return Application.getInstance(ZettelkastenApp.class);
    }

    public ByteArrayOutputStream getCurrentSessionLogs() {
        return inMemorySessionLog;
    }

    /**
     * Main method launching the application.
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(ZettelkastenApp.class, args);
    }

    @Override
    protected void initialize(String[] args) {
        this.params = args;
        super.initialize(args);
    }
}
