/*
 * Zettelkasten - nach Luhmann
 ** Copyright (C) 2001-2013 by Daniel Lüdecke (http://www.danielluedecke.de)
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

package de.danielluedecke.zettelkasten.database;

import de.danielluedecke.zettelkasten.CMakeFormImage;
import de.danielluedecke.zettelkasten.ZettelkastenView;
import de.danielluedecke.zettelkasten.util.classes.Comparer;
import de.danielluedecke.zettelkasten.util.Constants;
import de.danielluedecke.zettelkasten.util.HtmlUbbUtil;
import de.danielluedecke.zettelkasten.util.Tools;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JOptionPane;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.IllegalAddException;
import org.jdom2.IllegalDataException;
import org.jdom2.IllegalNameException;

/**
 * This is the data class. This class stores all the programme data in a JDOM
 * XML Tree. It also provides typical methods like getters and setters, stores
 * the file path, the modified state and so on. All relevant operations need for
 * managing the data file can be found here, except loading (open file) and saving
 * operations. These can be found in "CLoadSave.java".
 * 
 * @author danielludecke
 */
public class Daten {
    //
    // IMPORTANT!
    //
    // REMEMBER TO CHANGE THIS VERSION NUMBER WHEN DATA STRUCTURE HAS CHANGED!
    //
    /**
     * Constant for the current file version
     */
    private static final String currentVersion = "3.6";
    public static final String backwardCompatibleVersion = "3.4";
    /**
     * A refrence to the settings class
     */
    private final Settings settings;
    /**
     * A refrence to the settings class
     */
    private final BibTex bibtexObj;
    /**
     * A refrence to the synonyms class
     */
    private final Synonyms synonymsObj;
    /**
     * XML Document that Stores the main data
     */
    private Document zknFile;
    /**
     * XML Document that Stores the data of entries
     * that should be exportet to .zkn3-format
     */
    private Document zknFileExport;
    /**
     * XML Document that Stores the author data
     */
    private Document authorFile;
    /**
     * XML Document that Stores the keyword data
     */
    private Document keywordFile;
    /**
     * XML Document that Stores the meta information of the zettelkasten-data
     */
    private Document metainfFile;
    /**
     * Stores the index number of the currently displayed entry
     */
    private int zettelPos;
    /**
     * state variable that tracks changes to the data file
     */
    private boolean modified;
    /**
     * state variable that tracks changes to the meta-information-file
     */
    private boolean metamodified;
    /**
     * Indicates whether saving the data file was ok, or whether an error occured.
     */
    private boolean saveOk;
    /**
     * This array stores all watched entries in the order the user "surfed"
     * through the entries, so we have a history-function. The user can then go
     * back to previously accessed entries and so on...
     */
    private int[] history;
    /**
     * Indicates the current position in that array, i.e. when the user activates
     * the history function, we have to know which element of the history array is
     * currently "active".
     */
    private int historyPosition;
    /**
     * The array's maximum limit does not automatically equal the amount of saved
     * history steps. so we use this as internal counter.
     */
    private int historyCount;
    /**
     * Stores the files which we want to retrieve from the main data file (filename.zkn3).
     * This file is a zip-container with the file-extension ".zkn3" and contains several XML-Files.
     * We cannot retrieve those file simply with the method "zip.getNextEntry()", since the SAXBuilder
     * closes the zip-inputstream. To retrieve all XML-files from within the zip-file, without saving
     * them temporarily to harddisk(!), we need to re-open the zip-container again for each file.
     * 
     * See class "CLoadDialog.java" for more details.
     */
    private final List<String> filesToLoad = new ArrayList<String>();
    /**
     * This list stores all follower and follower's follower of an entry
     */
    private final List<Integer> allLuhmannNumbers = new ArrayList<Integer>();
    /**
     * here we store whether the keyword list (keywordFile)
     * is up to date or not. if it's up to date, we do not need
     * to start the CShowKeywordList task, when the user switches
     * to that tab in the main window's tab pane
     */
    private boolean keywordlistUpToDate;
    /**
     * here we store whether the author list (authorFile)
     * is up to date or not. if it's up to date, we do not need
     * to start the CShowAuthorList task, when the user switches
     * to that tab in the main window's tab pane
     */
    private boolean authorlistUpToDate;
    /**
     * here we store whether the title list (titles of entries)
     * is up to date or not. if it's up to date, we do not need
     * to start the CShowTitleList task, when the user switches
     * to that tab in the main window's tab pane
     */
    private boolean titlelistUpToDate;
    /**
     * here we store whether the cluster list (related keywords)
     * is up to date or not. if it's up to date, we do not need
     * to rebuild that list/tree, when the user switches
     * to that tab in the main window's tab pane
     */
    private boolean clusterlistUpToDate;
    /**
     * here we store whether the attachment-list (entries' attachments)
     * is up to date or not. if it's up to date, we do not need
     * to rebuild that list/tree, when the user switches
     * to that tab in the main window's tab pane
     */
    private boolean attachmentlistUpToDate;
    /**
     * This variables stored the ID of the last added entry.
     */
    private String lastAddedZettelID = null;
    //
    // constant variables
    //
    /**
     * Constant used as parameter for the getCount method
     * Retrieves the count of elements of the main data file (zknfile)
     */
    public static final int ZKNCOUNT = 1;
    /**
     * Constant used as parameter for the getCount method
     * Retrieves the count of elements of the keyword file (keywordfile)
     */
    public static final int KWCOUNT = 2;
    /**
     * Constant used as parameter for the getCount method
     * Retrieves the count of elements of the author file (authorfile)
     */
    public static final int AUCOUNT = 3;
    /**
     * One of the return values when adding an entry
     * (see {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry()}).
     * <br><br>
     * This value indicates that everything was OK when adding the entry.
     */
    public static final int ADD_ENTRY_OK = 1;
    /**
     * One of the return values when adding an entry as follower (trailing-entry)
     * (see {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry()}).
     * <br><br>
     * This value indicates that everything was OK when adding the follower-entry.
     */
    public static final int ADD_LUHMANNENTRY_OK = 2;
    /**
     * One of the return values when adding an entry
     * (see {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry()}).
     * <br><br>
     * This value indicates that a general error occured when adding the entry.
     */
    public static final int ADD_ENTRY_ERR = 3;
    /**
     * One of the return values when adding an entry as follower (trailing-entry)
     * (see {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry()}).
     * <br><br>
     * This value indicates that adding the entry in general was OK, but that this entry <i>could not</i> be
     * added as follower-entry, e.g. in case the parent-entry already had such an index-number as follower...
     */
    public static final int ADD_LUHMANNENTRY_ERR = 4;
    /**
     * Indicates the maximum amount of saved history steps
     */
    final int HISTORY_MAX = 50;
    /**
     * Reference to the main frame.
     */
    private final ZettelkastenView zknframe;

    public static final String DOCUMENT_ZETTELKASTEN = "zettelkasten";
    public static final String DOCUMENT_AUTHORS = "authors";
    public static final String DOCUMENT_KEYWORDS = "keywords";

    public static final String ELEMENT_ENTRY = "entry";
    public static final String ELEMENT_ZETTEL = "zettel";
    public static final String ELEMENT_TITLE = "title";
    public static final String ELEMENT_CONTENT = "content";
    public static final String ELEMENT_KEYWORD = "keywords";
    public static final String ELEMENT_AUTHOR = "author";
    public static final String ELEMENT_AUTHORS = "authors";
    public static final String ELEMENT_REMARKS = "misc";
    public static final String ELEMENT_MANLINKS = "manlinks";
    public static final String ELEMENT_ATTACHMENTS = "links";
    public static final String ELEMENT_ATTCHILD = "link";
    public static final String ELEMENT_TRAILS = "luhmann";
    public static final String ELEMENT_LUHMANN_NUMBER = "luhindex";
    public static final String ELEMEMT_DESCRIPTION = "description";

    public static final String ELEMENT_ATTACHMENT_PATH = "attachmentpath";
    public static final String ELEMENT_IMAGE_PATH = "imagepath";
    public static final String ELEMENT_VERSION_INFO = "version";
    
    // attributes of the "zettel" element
    public static final String ATTRIBUTE_RATING = "rating";
    public static final String ATTRIBUTE_RATINGCOUNT = "ratingcount";
    public static final String ATTRIBUTE_TIMESTAMP_CREATED = "ts_created";
    public static final String ATTRIBUTE_TIMESTAMP_EDITED = "ts_edited";
    public static final String ATTRIBUTE_ZETTEL_ID = "zknid";
    public static final String ATTRIBUTE_NEXT_ZETTEL = "nextzettel";
    public static final String ATTRIBUTE_PREV_ZETTEL = "prevzettel";

    public static final String ATTRIBUTE_FIRST_ZETTEL = "firstzettel";
    public static final String ATTRIBUTE_LAST_ZETTEL = "lastzettel";
    
    // attributes of keyword and author elements
    public static final String ATTRIBUTE_AUTHOR_ID = "authid";
    public static final String ATTRIBUTE_KEYWORD_ID = "keywid";
    public static final String ATTRIBUTE_AUTHOR_TIMESTAMP = "authts";
    public static final String ATTRIBUTE_AUTHOR_BIBKEY = "bibkey";
    public static final String ATTRIBUTE_KEYWORD_TIMESTAMP = "keywts";
    public static final String ATTRIBUTE_FREQUENCIES = "f";
    
    /**
     * The file format of the zknFile<br>
     * <br>
     * root element: <b>zettelkasten</b><br>
     * <br>
     * <u>attributes</u> of the <b>zettelkasten</b> element:<br>
     * <i>firstzettel</i> - a reference to the first entry (number) in the display order<br>
     * <i>lastzettel</i> - a reference to the last entry (number) in the display order<br>
     * <br>
     * <u>children</u> of the <b>zettelkasten</b> element:<br>
     * <ul>
     *  <li><b>zettel</b><br>
     *      An ID indicating the fixed position of a "zettel" (entry) is not needed,
     *      since new entries which are e.g. inserted "in between", will be added to the
     *      end of the xml file. However, we have a unique ID for each entry for
     *      synchronization of different data bases etc.
     *      <br>
     *      <br>
     *      <u>attributes</u>:
     *      <ul>
     *          <li><i>zknid</i> - a unique ID for this entry</li>
     *          <li><i>nextzettel</i> - reference to the next entry (number) in the display order</li>
     *          <li><i>prevzettel</i> - reference to the previous entry (number) in the display order</li>
     *          <li><i>rating</i> - (optional) the personal rating for this entry</li>
     *          <li><i>ratingcount</i> - (optional) counts how often this entry has been rated</li>
     *          <li><i>ts_created</i> - the timestamp when this entry was first created</li>
     *          <li><i>ed_edited</i> - the timestamp when this entry was last modified</li>
     *      </ul>
     *  </li>
     * </ul>
     * 
     * <br>
     * <u>children</u> of <b>zettel</b><br>
     * <ul>
     *  <li><i>title</i> - the title of the entry</li>
     *  <li><i>content</i> - the content of each slip, the main text. may contain html-syntax due to formatting.</li>
     *  <li><i>author</i> - one or more index number(s) indicating the entry of the XML-data-file "authorFile", separated by commas</li>
     *  <li><i>keywords</i> - one or more index number(s) indicating the entry of the XML-data-file "keywordFile", separated by commas</li>
     *  <li><i>manlinks</i> - manual links from the user, referring to other entries, i.e. their index numbers</li>
     *  <li><i>links</i> - list of links (attachments) to files or websites, separated by new sub-elements called "link"
     *      <ul>
     *          <li><i>link</i> - the single entriy (attachment) of the links</li>
     *          <li><i>link</i> - ...</li>
     *      </ul>
     *  </li>
     *  <li><i>misc</i> - miscellaneous or orther comments, text, etc.</li>
     *  <li><i>luhmann</i> - used to indicate follower-entries (trails). follower-entries are dispayed as "sub-entries" of an entry, thus enabling a structured overview of entries.</li>
     * </ul>
     * <br>
     * <br>
     * Now, the strings/content related to the index numbers in "author" and "keywords" are
     * stored in separetd XML files: authorFile and keywordFile (see variable declaration above).<br>
     * <br>
     * Sample of a possible authorFile:<br>
     * <br>
     * &lt;authors&gt;<br>
     * &nbsp;&nbsp;&lt;entry f="9"&gt;Luhmann, Niklas (1984): Soziale Systeme. Frankfurt/Main: Suhrkamp&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry f="3" bibkey="bae2005"&gt;Baecker, Dirk (2005): Form und Formen der Kommunikation. Frankfurt/Main: Suhrkamp&lt;/entry&gt;<br>
     * &lt;/authors&gt;<br>
     * <br>
     * <br>
     * Sample of a possible keywordFile:<br>
     * <br>
     * &lt;keywords&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;Soziale Systeme&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;Habermas&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;Grundlagen Systemtheorie&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;Phänomenologie&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;Wissenschaft&lt;/entry&gt;<br>
     * &nbsp;&nbsp;&lt;entry&gt;System und Umwelt&lt;/entry&gt;<br>
     * &lt;/keywords&gt;<br>
     * @param zkn
     * @param s
     * @param syn
     * @param bib
     */
    public Daten(ZettelkastenView zkn, Settings s, Synonyms syn, BibTex bib) {
        // initiate the JDOM files and all other data, thus
        // creating an empty "Zettelkasten"
        zknframe = zkn;
        settings = s;
        synonymsObj = syn;
        bibtexObj = bib;
        zettelPos = 1;
        initZettelkasten();
    }


    // TODO prüfen, ob überall, wo notwendig, author-/keyword ID und timestamp attribute aktualisiert werden
    
    /**
     * Initiates the global variables and creates empty JDom objects
     * <br><br>
     * <b>Warning!</b> This method does <i>not</i> clear the filePath-variable, so
     * be sure you have set the filePath to null manually, if necessary.
     */
    public final void initZettelkasten() {
        // reset all global variables
        modified = false;
        zknframe.resetBackupNecessary();
        zknFile=null;
        authorFile=null;
        keywordFile=null;
        metainfFile=null;
        zknFileExport=null;
        // init the history array
        history = new int[HISTORY_MAX];
        // current position in the history array refers to the first element
        historyPosition = 0;
        // indicates that we have one (initial) element
        historyCount = 1;
        // the one and only element is the first entry
        history[0] = 1;
        // no update to the tabbed panes in the main window when nothing is loaded
        keywordlistUpToDate = true;
        authorlistUpToDate = true;
        titlelistUpToDate=true;
        clusterlistUpToDate=true;
        attachmentlistUpToDate=true;
        // create "empty" XML JDom objects
        zknFile = new Document(new Element(DOCUMENT_ZETTELKASTEN));
        authorFile = new Document(new Element(DOCUMENT_AUTHORS));
        keywordFile = new Document(new Element(DOCUMENT_KEYWORDS));
        // prepare the metainformation-file
        metainfFile = new Document(new Element("metainformation"));
        // first create an attribute for the fileversion-number
        Element fileversion = new Element(ELEMENT_VERSION_INFO);
        fileversion.setAttribute("id", currentVersion);
        // and add it to the document
        metainfFile.getRootElement().addContent(fileversion);
        // than create an empty description and add it
        Element desc = new Element(ELEMEMT_DESCRIPTION);
        metainfFile.getRootElement().addContent(desc);
        // than create an empty atachment-path and add it
        Element attpath = new Element(ELEMENT_ATTACHMENT_PATH);
        metainfFile.getRootElement().addContent(attpath);
        // than create an empty atachment-path and add it
        Element imgpath = new Element(ELEMENT_IMAGE_PATH);
        metainfFile.getRootElement().addContent(imgpath);
        // init zettel-position-index
        zettelPos = 1;
        // reset references to first and last entry
        setFirstZettel(-1);
        setLastZettel(-1);
        // here we add all files which are stored in the zipped data-file in a list-array
        filesToLoad.clear();
        filesToLoad.add(Constants.metainfFileName);
        filesToLoad.add(Constants.zknFileName);
        filesToLoad.add(Constants.authorFileName);
        filesToLoad.add(Constants.keywordFileName);
        filesToLoad.add(Constants.bookmarksFileName);
        filesToLoad.add(Constants.searchrequestsFileName);
        filesToLoad.add(Constants.desktopFileName);
        filesToLoad.add(Constants.desktopModifiedEntriesFileName);
        filesToLoad.add(Constants.desktopNotesFileName);
        filesToLoad.add(Constants.synonymsFileName);
        filesToLoad.add(Constants.bibTexFileName);
        // reset list
        allLuhmannNumbers.clear();
    }


    /**
     * This method returns the version of the fileformat. the filestructure and data-storing
     * might change due to further development of this programm, so here we can check
     * for the current fileformat-version if necessary. This information is stored in the
     * metainformation-file.
     * <br><br>
     * This may differ from the version of the <i>current</i> fileformat. see
     * {@link #getCurrentVersionInfo() getCurrentVersionInfo()} to retrieve the version-number
     * of the current fileformat.
     * 
     * @return a string containing the version-number of the zettelkasten-file-format, or {@code null} if
     * no such attribute exists
     */
    public String getVersionInfo() {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_VERSION_INFO);
        // check whether it's null or not
        if (null==el) {
            // log error
            Constants.zknlogger.log(Level.WARNING,"Could not read file version info. XML-element is null!");
            return null;
        }
        // get id-attribute
        String id = el.getAttributeValue("id");
        // check for valid value
        if (null==id || id.isEmpty()) {
            // log error
            Constants.zknlogger.log(Level.WARNING,"Could not read file version info. XML-attribute is null!");
            return null;
        }
        // return the attribute value
        return id;
    }
    

    /**
     * Retrieves the user defined path to attachments
     * @return the user defined path to attachments
     */
    public File getUserAttachmentPath() {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_ATTACHMENT_PATH);
        // check whether it's null or not
        if (null==el || el.getText().trim().isEmpty()) {
            return null;
        }
        // else return the attribute value
        return new File(el.getText());
    }
    /**
     * Ssaves the user defined path to attachments
     * @param path the user defined path to attachments
     */
    public void setUserAttachmentPath(String path) {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_ATTACHMENT_PATH);
        // check whether it's null or not
        if (null==el) {
            // than create an empty atachment-path and add it
            el = new Element(ELEMENT_ATTACHMENT_PATH);
            metainfFile.getRootElement().addContent(el);
        }
        // check for valid parameter
        if (path!=null) {
            // set new path
            el.setText(path);
            // and change modified state
            setMetaModified(true);
        }
    }


    /**
     * Retrieves the user defined path to images
     * @return the user defined path to images
     */
    public File getUserImagePath() {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_IMAGE_PATH);
        // check whether it's null or not
        if (null==el || el.getText().trim().isEmpty()) {
            return null;
        }
        // else return the attribute value
        return new File(el.getText());
    }
    /**
     * Ssaves the user defined path to images
     * @param path the user defined path to images
     */
    public void setUserImagePath(String path) {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_IMAGE_PATH);
        // check whether it's null or not
        if (null==el) {
            // than create an empty atachment-path and add it
            el = new Element(ELEMENT_IMAGE_PATH);
            metainfFile.getRootElement().addContent(el);
        }
        // check for valid parameter
        if (path!=null) {
            // set new path
            el.setText(path);
            // and change modified state
            setMetaModified(true);
        }
    }

    
    /**
     * This method returns the current (latest) version of the fileformat. This may differ from
     * the version of the <i>loaded</i> file. see {@link #getVersionInfo() getVersionInfo()} to retrieve
     * the version-number of the loaded fileformat.
     *
     * @return a string containing the current (latest) version-number of the zettelkasten-file-format
     */
    public String getCurrentVersionInfo() {
        // return the current version info
        return currentVersion;
    }


    /**
     * Set and Get the modified state of the meta-information
     * @return 
     */
    public boolean isMetaModified() {
        return metamodified;
    }
    /**
     * Set and Get the modified state of the meta-information
     * @param m
     */
    public void setMetaModified(boolean m) {
        metamodified = m;
        zknframe.setBackupNecessary();
    }


    /**
     * Set and Get the modified state of the file
     * @return 
     */
    public boolean isModified() {
        return modified;
    }
    /**
     * Set and Get the modified state of the file
     * @param m
     */
    public void setModified(boolean m) {
        modified = m;
        zknframe.setBackupNecessary();
    }
    /**
     * Returns the size of this list. This list stores the xml-files which
     * should be retrieved from the compressed main-datafile. See class
     * CLoadDialog.java for more details.
     * @return the amount of files to load from the main datafile
     */
    public int getFilesToLoadCount() {
        return filesToLoad.size();
    }
    /**
     * Returns the filename of the xml-datafiles we want to retrieve from our
     * compressed main-datafile. See class CLoadDialog.java for more details.
     * @param index (the element which should be retrieved)
     * @return (the string containing the filename of the xml-file we want to have)
     */
    public String getFileToLoad(int index) {
        return filesToLoad.get(index);
    }
    
    
    /**
     * Set and Get the whole main data
     * (Zettelkasten only, without Author and Keyword lists)
     * @param zkd (zettelkasten xml datafile)
     */
    public void setZknData(Document zkd) {
        zknFile = zkd;
        setModified(true);
    }
    /**
     * Set and Get the whole main data
     * (Zettelkasten only, without Author and Keyword lists)
     * @return zettelkasten xml datafile
     */
    public Document getZknData() {
        return zknFile;
    }
    

    /**
     * This method checks whether the current fileformat is of a <b>newer</b> version than the loaded data-file.
     * if so, we have to convert the data into the new fileformat. use {@link #getVersionInfo() getVersionInfo()}
     * and {@link #getCurrentVersionInfo() getCurrentVersionInfo()} to retrieve the version numbers of the loaded
     * and current file-format.<br><br>
     * <b>Important!</b> Use {@link #updateVersionInfo() updateVersionInfo()} to update the version-setting of the
     * loaded data-file.
     *
     * @return {@code true} if the current, latest file-format is of a newer version than the loaded data-file.
     * {@code false} if the loaded data-file is uptodate.
     */
    public boolean isNewVersion() {
        // get version info
        String verinfo = getVersionInfo();
        // check for valid value
        if (verinfo!=null && !verinfo.isEmpty()) {
            // get data-version of loaded file
            float lv = Float.parseFloat(verinfo);
            // get current fileversion
            float cv = Float.parseFloat(currentVersion);
            // check whether the current data-version is newer than the loaded one
            return (lv<cv);
        }
        // log error
        Constants.zknlogger.log(Level.WARNING,"Check for new file version failed. Could not read version info!");
        return false;
    }


    /**
     * This method checks whether the current fileformat is of an <b>older</b> version than the loaded data-file.
     * This might be the case, if the loaded data-file was saved with a newer program-version than the currently
     * used program.<br><br>
     * If so, we have to tell the user that the file-format is not supported and cannot be opened with the
     * current program-version.
     *
     * @return {@code true} if the current program-version cannot read the loaded data-file because it was saved
     * with a newer program-version. {@code false} if the loaded data-file can be read.
     */
    public boolean isIncompatibleFile() {
        // get version info
        String verinfo = getVersionInfo();
        // check for valid value
        if (verinfo!=null && !verinfo.isEmpty()) {
            // get data-version of loaded file
            float lv = Float.parseFloat(verinfo);
            // get current fileversion
            float cv = Float.parseFloat(currentVersion);
            // check whether the current data-version is newer than the loaded one
            return (lv>cv);
        }
        // log error
        Constants.zknlogger.log(Level.WARNING,"Could not check for data compatibility. File version could not be read!");
        return false;
    }


    /**
     * This method appends a document with zettelkasten-data to an existing
     * document.<br><br>
     * This method is used when importing data. The imported data is appended
     * to an existing, opened data file.
     * 
     * @param zkd the zettelkasten-data in xml-document-format
     */
    public void appendZknData(Document zkd) {
        // get current count, so we know at which position new entry were added
        // to the XML-document
        int currentpos = getCount(ZKNCOUNT);
        // create zettel element
        Element zettel;
        // dummy variable
        boolean mod = false;
        // remove each element. we need to use the remove-content-method, because
        // this detached the element from its former parent, so it can be added
        // to the new main-data-file
        // check whether we have any content left...
        while (zkd.getRootElement().getContentSize()>0) {
            // try to remove/detach the child-element
            if ((zettel=(Element)zkd.getRootElement().removeContent(0))!=null) {
                // check whether the imported entry is empty or not
                if (zettel.getChild(ELEMENT_CONTENT)!=null && !zettel.getChild(ELEMENT_CONTENT).getText().isEmpty()) {
                    try {
                        zknFile.getRootElement().addContent(zettel);
                        // set modified flag
                        mod = true;
                    }
                    catch (IllegalDataException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalAddException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                }
            }
        }
        // check whether we have added any new entries
        // if so, re-convert IDs to numbers
        if (mod) {
            // go through all new added entries
            for (int cnt=currentpos+1; cnt<=getCount(ZKNCOUNT); cnt++) {
                // retrieve each new added entry
                zettel = retrieveZettel(cnt);
                // set back reference to current last entry
                setPrevZettel(cnt, getLastZettel());
                // set pointer from current last entry to this new imported/added entry
                setNextZettel(getLastZettel(),cnt);
                // set pointer from first entry to this entry
                setPrevZettel(getFirstZettel(), cnt);
                // set next pointer from this entry to first entry
                setNextZettel(cnt, getFirstZettel());
                // set this entry as new last entry
                setLastZettel(cnt);
                //
                // here we change the entry's luhmann-numbers (trailing entries) and the
                // entry's manual links with the unique IDs
                //
                replaceAttributeIDWithNr(zettel);
                //
                // here we convert back the author IDs in footnote references
                // to the related author index numbers...
                //
                // retrieve content of entry and convert all author footnotes, which
                // contain author-index-numbers, into the related author-IDs.
                String content = zettel.getChild(ELEMENT_CONTENT).getText();
                // check for footnotes
                int pos = 0;
                while (pos!=-1) {
                    // find the html-tag for the footnote
                    pos = content.indexOf(Constants.FORMAT_FOOTNOTE_OPEN, pos);
                    // if we found something...
                    if (pos!=-1) {
                        // find the closing quotes
                        int end = content.indexOf("]", pos+2);
                        // if we found that as well...
                        if (end!=-1) {
                            try {
                                // extract footnote-number
                                String fn = content.substring(pos+4, end);
                                // retrieve author ID from related footnote number
                                try {
                                    int authorNr = getAuthorNumberFromID(fn);
                                    // replace author number with author ID inside footnote
                                    content = content.substring(0, pos+4) + String.valueOf(authorNr) + content.substring(end);
                                }
                                catch (NumberFormatException ex) {
                                    // log error
                                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                                    Constants.zknlogger.log(Level.WARNING,"Could not convert author ID into author number!");
                                }
                            }
                            catch (IndexOutOfBoundsException ex) {
                            }
                            // and add it to the linked list, if it doesn't already exist
                            // set pos to new position
                            pos = end;
                        }
                        else {
                            pos = pos+4;
                        }
                    }
                }
                // check for manual links
                pos = 0;
                while (pos!=-1) {
                    // find the html-tag for the manual link
                    pos = content.indexOf(Constants.FORMAT_MANLINK_OPEN, pos);
                    // if we found something...
                    if (pos!=-1) {
                        // find the closing quotes
                        int end = content.indexOf("]", pos+2);
                        // if we found that as well...
                        if (end!=-1) {
                            try {
                                // extract manual-link--number
                                String ml = content.substring(pos+3, end);
                                // retrieve entry ID from related manual link number
                                try {
                                    int zetNr = getZettelNumberFromID(ml);
                                    // replace author number with author ID inside footnote
                                    content = content.substring(0, pos+3) + String.valueOf(zetNr) + content.substring(end);
                                }
                                catch (NumberFormatException ex) {
                                    // log error
                                    Constants.zknlogger.log(Level.WARNING,"Could not convert entry ID into related manual link number!");
                                }
                            }
                            catch (IndexOutOfBoundsException ex) {
                            }
                            // and add it to the linked list, if it doesn't already exist
                            // set pos to new position
                            pos = end;
                        }
                        else {
                            pos = pos+3;
                        }
                    }
                }
                // set back changes
                zettel.getChild(ELEMENT_CONTENT).setText(content);
            }
        }
        // change modified state
        if (mod) {
            setModified(true);
        }
    }


    /**
     * Set and Get the authorlist
     * @param ald authorlist xml datafile
     */
    public void setAuthorData(Document ald) {
        authorFile = ald;
        setModified(true);
    }
    /**
     * Set and Get the authorlist
     * @return authorlist xml datafile
     */
    public Document getAuthorData() {
        return authorFile;
    }
    

    /**
     * Set and Get the keyword list
     * @param kld keyword xml datafile
     */
    public void setKeywordData(Document kld) {
        keywordFile = kld;
        setModified(true);
    }
    /**
     * This method returns the keyword data file as JDOM document
     * @return the keyword data file as JDOM document
     */
    public Document getKeywordData() {
        return keywordFile;
    }


    /**
     * Set and Get the metainformation of the zettelkasten-data
     * @param mid metainformation xml datafile
     */
    public void setMetaInformationData(Document mid) {
        metainfFile = mid;
        setMetaModified(true);
    }
    /**
     * This method returns the metainformation of the zettelkasten-data as JDOM document
     * @return the metainformation as JDOM document
     */
    public Document getMetaInformationData() {
        return metainfFile;
    }
    
    
    /**
     * Set the whole zettelkasten
     * (Zettelkasten with Author and Keyword lists)
     * 
     * @param zkd a zettelkasten xml datafile
     * @param ald an authorlist xml datafile
     * @param kld a keywordlist xml datafile
     * @param mid
     */
    public void setCompleteZknData(Document zkd, Document ald, Document kld, Document mid) {
        zknFile = zkd;
        authorFile = ald;
        keywordFile = kld;
        metainfFile = mid;
        setModified(true);
    }

    
    /**
     * This method returns the description of the zettelkasten-data, which is stored
     * in the metainformation-file of the zipped data-file. Usually needed when showing
     * information on the opened datafile
     * 
     * @return a string with the description of this zettelkasten
     */
    public String getZknDescription() {
        // get the child element
        Element el = metainfFile.getRootElement().getChild(ELEMEMT_DESCRIPTION);
        // check whether it's null
        if (null==el) {
            return "";
        }
        // else return element-text
        return el.getText();
    }
    /**
     * This method sets the description of the zettelkasten-data, which is stored
     * in the metainformation-file of the zipped data-file.
     * 
     * @param desc a string with the description of this zettelkasten
     * @return 
     */
    public boolean setZknDescription(String desc) {
        // get the element
        Element el = metainfFile.getRootElement().getChild(ELEMEMT_DESCRIPTION);
        try {
            // check whether element exists
            if (null==el) {
                // if element does not exist, create it
                el = new Element(ELEMEMT_DESCRIPTION);
                // and add it to the meta-xml-file
                metainfFile.getRootElement().addContent(el);
            }
            // finally, set the text
            el.setText(desc);
            // change modified state
            setMetaModified(true);
        }
        catch (IllegalAddException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            return false;
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            return false;
        }
        // return success
        return true;
    }
    /**
     * This method adds another description of zettelkasten-data to the existing one.
     * Usually this is need when appending Zettelkasten-datafiles.
     * 
     * @param desc
     */
    public void addZknDescription(String desc) {
        // if description is not empty, concatenate it to old description
        if (!desc.isEmpty()) {
            if (setZknDescription(getZknDescription()+System.getProperty("line.separator")+System.getProperty("line.separator")+desc)) {
                setMetaModified(true);
            }
        }
    }


    /**
     * This method changes the frequencies of an entry's authors and keywords by the given value {@code addvalue}.
     * @param nr the entrynumber, which author- and keywords-frequencies should be changed
     * @param addvalue the amount of increase or decrease of each author/keyword-frequency
     */
    private void changeFrequencies(int nr, int addvalue) {
        // first of all, we duplicate all authors and keywords frequencies from the existing entry.
        // therefore, we first retrieve all author-index-numbers from that entry
        int[] aus = getAuthorIndexNumbers(nr);
        // check whether we have any values at all
        if (aus!=null && aus.length>0) {
            // iterate the array
            for (int a : aus) {
                // check for valid value
                if (a!=-1) {
                    try {
                        // retrieve existing author
                        Element au = retrieveElement(authorFile, a);
                        // chek for valid value
                        if (au!=null) {
                            // get the count-value, which indicates the frequency of occurences of this
                            // author in the whole data file
                            String freq = au.getAttributeValue(ATTRIBUTE_FREQUENCIES);
                            if (freq!=null) {
                                int f = Integer.parseInt(freq);
                                // increase frequency by 1
                                au.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(f+addvalue));
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalNameException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalDataException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                }
            }
        }
        // now do this for the keywords. retrieve all keyword -index-numbers from that entry
        int[] kws = getKeywordIndexNumbers(nr);
        // check whether we have any values at all
        if (kws!=null && kws.length>0) {
            // iterate the array
            for (int k : kws) {
                // check for valid value
                if (k!=-1) {
                    try {
                        // retrieve existing author
                        Element kw = retrieveElement(keywordFile, k);
                        // chek for valid value
                        if (kw!=null) {
                            // get the count-value, which indicates the frequency of occurences of this
                            // keyword in the whole data file
                            String freq = kw.getAttributeValue(ATTRIBUTE_FREQUENCIES);
                            if (freq!=null) {
                                int f = Integer.parseInt(freq);
                                // increase frequency by 1
                                kw.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(f+addvalue));
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalNameException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalDataException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                }
            }
        }
    }


    /**
     * This method duplicates an entry and inserts it at the end or the next empty place in the
     * data file
     * 
     * @param nr the number of the entry that should be duplicated
     * @return 
     */
    public boolean duplicateEntry(int nr) {
        // first of all, we duplicate all authors and keywords frequencies from the existing entry.
        // therefore, we first retrieve all author-index-numbers from that entry
        changeFrequencies(nr,1);
        // retrieve entry that should be duplicated
        Element oldzettel = retrieveElement(zknFile,nr);
        // create new zettel
        Element zettel = new Element(ELEMENT_ZETTEL);
        // check whether we have any empty elements in between where we can insert the new entry
        int emptypos = retrieveFirstEmptyEntry();
        // if we have any empty elements...
        if (emptypos!=-1 && settings.getInsertNewEntryAtEmpty()) {
            // retrieve empty element
            zettel = retrieveElement(zknFile, emptypos);
            // and remove former content, so we can add new content
            zettel.removeContent();
        }
        try {
            setZettelID(zettel);
            //
            // add title
            //
            // create child element with title information
            Element t = new Element(ELEMENT_TITLE);
            // and add it to the zettel-element
            zettel.addContent(t);
            // set value of the child element
            t.setText(oldzettel.getChild(ELEMENT_TITLE).getText());
            //
            // add content
            //
            // create child element with content information
            Element c = new Element(ELEMENT_CONTENT);
            // and add it to the zettel-element
            zettel.addContent(c);
            // set value of the content element
            c.setText(oldzettel.getChild(ELEMENT_CONTENT).getText());
            //
            // add author
            //
            // create child element with author information
            Element a = new Element(ELEMENT_AUTHOR);
            // and add it to the zettel-element
            zettel.addContent(a);
            // set value of author element
            a.setText(oldzettel.getChild(ELEMENT_AUTHOR).getText());
            //
            // add keywords
            //
            // create child element with keyword information
            Element k = new Element(ELEMENT_KEYWORD);
            // and add it to the zettel-element
            zettel.addContent(k);
            // store keyword index numbers
            k.setText(oldzettel.getChild(ELEMENT_KEYWORD).getText());
            //
            // now comes the manual links to other entries
            //
            Element m = new Element(ELEMENT_MANLINKS);
            zettel.addContent(m);
            m.setText("");
            //
            // add hyperlinks
            //
            // create child element with link information
            Element h = new Element(ELEMENT_ATTACHMENTS);
            // and add it to the zettel-element
            zettel.addContent(h);
            // add each hyperlink. therefor, iterate the array
            List<Element> links = oldzettel.getChild(ELEMENT_ATTACHMENTS).getChildren();
            Iterator<Element> i = links.iterator();
            while (i.hasNext()) {
                // create a new subchuld-element
                Element sublink = new Element(ELEMENT_ATTCHILD);
                Element le = i.next();
                // and add the link-string from the array
                sublink.setText(le.getText());
                h.addContent(sublink);
            }
            //
            // add remarks
            //
            // create child element with content information
            Element r = new Element(ELEMENT_REMARKS);
            // and add it to the zettel-element
            zettel.addContent(r);
            // set value of the content element
            r.setText(oldzettel.getChild(ELEMENT_REMARKS).getText());
            //
            // add timestamp
            //
            // set creation timestamp, but set no text for edit timestamp
            // since the entry is not edited
            setTimestamp(zettel, Tools.getTimeStamp(), "");
            //
            // now comes the luhmann number
            //
            Element l = new Element(ELEMENT_TRAILS);
            zettel.addContent(l);
            l.setText(oldzettel.getChild(ELEMENT_TRAILS).getText());
            //
            // complete datafile
            //
            // if we have any empty elements, go on here
            if (emptypos!=-1 && settings.getInsertNewEntryAtEmpty()) {
                // return the empty-position, which is now filled with the new author-value
                zettelPos = emptypos;
            }
            else {
                // finally, add the whole element to the data file
                zknFile.getRootElement().addContent(zettel);
                // set the zettel-position to the new entry
                zettelPos=getCount(ZKNCOUNT);
            }
            // duplicate this entry into the correct entry order
            // by changing the prev/nex references (or pointers) of the entries.
            changeZettelPointer(zettelPos, nr);
            // titles have to be updated.
            setTitlelistUpToDate(false);
            // set modified state
            setModified(true);
        }
        catch (IllegalAddException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return false;
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return false;
        }
        return true;
    }

    
    /**
     * This function retrieves an element of a xml document at a given
     * position. used for other methods like getAuthor or
     * getKeyword.<br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param doc the xml document where to look for elements. use following parameters:<br>
     * - {@link #authorFile authorFile}<br>
     * - {@link #keywordFile keywordFile}<br>
     * - {@link #zknFile zknFile}
     * @param pos the position of the element. must be a value from <b>1</b> to
     * {@link #getCount(int) getCount()}.
     * @return the element if a match was found, otherwise {@code null}
     */
    private Element retrieveElement(Document doc, int pos) {
        // create a list of all elements from the given xml file
        try { 
            List<?> elementList = doc.getRootElement().getContent();
            // and return the requestet Element
            try {
                return (Element) elementList.get(pos-1);
            }
            catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return null;
        }
    }


    /**
     * This method updates the version-information of the loaded file to the latest version number.
     */
    public void updateVersionInfo() {
        // retrieve version-element
        Element el = metainfFile.getRootElement().getChild(ELEMENT_VERSION_INFO);
        // check whether it's null or not
        if (el!=null) {
            el.setAttribute("id",currentVersion);
        }
    }


    /**
     * This function retrieves an element of a xml document at a given
     * position. used for the export of entries, for instance.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param pos the position of the element, ranged from 1 to {@link #getCount(int) getCount(ZKNCOUNT)}
     * @return the element if a match was found, otherwise {@code null}
     */
    public Element retrieveZettel(int pos) {
        return retrieveElement(zknFile,pos);
    }


    /**
     * This method returns the position of a keyword in the keyword XML file {@link #keywordFile}.
     * if the keyword doesn't exist, the return value is {@code -1}.
     *
     * @param kw keyword which is searched for in the keyword list
     * @return the position of the author string or -1 if no match was found
     */
    public int findKeywordInDatabase(String kw) {
        return getKeywordPosition(kw, true);
    }


    /**
     * This method returns the position of a keyword in the keyword XML file {@link #keywordFile}.
     * if the keyword doesn't exist, the return value is {@code -1}.
     * 
     * @param kw keyword which is searched for in the keyword list
     * @param matchcase whether the keyword-search is case-sensitive ({@code true}) or not ({@code false})
     * @return the position of the author string or -1 if no match was found
     */
    public int getKeywordPosition(String kw, boolean matchcase) {
        // check for valid value
        if (null==kw || kw.trim().isEmpty()) return -1;
        // create a list of all keyword elements from the keyword xml file
        try {
            List<?> keywordList = keywordFile.getRootElement().getContent();
            // and an iterator for the loop below
            Iterator<?> iterator = keywordList.iterator();
            // counter for the return value if a found author matches the parameter
            int cnt = 1;
            // iterate loop
            while (iterator.hasNext()) {
                // retrieve each single element
                Element keyword = (Element) iterator.next();
                // if keyword matches the parameter string, return the position
                if (matchcase&&kw.equals(keyword.getText())) {
                    return cnt;
                }
                else if (!matchcase&&kw.equalsIgnoreCase(keyword.getText())) {
                    return cnt;
                }
                // else increase counter
                cnt++;
            }
            // if no keyword was found, return -1
            return -1;
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return -1;
        }
    }


    /**
     * This method returns the author-index-number of that author-value, that contains the
     * bibkey (i.e. the "bibkey"-attribute) given in {@code bibkey}.
     * 
     * @param bibkey the bibkey which position has to be found
     * @return the author-index-number (i.e. author-position in the authorFile.xml) of that author
     * which bibkey-attribute matches (case-sensitive!) the parameter {@code bibkey}, or {@code -1} if
     * no author with that bibkey-value was found.
     */
    public int getBibkeyPosition(String bibkey) {
        // check for valid value
        if (null==bibkey || bibkey.trim().isEmpty()) return -1;
        // create a list of all author elements from the author xml file
        try {
            List<?> authorList = authorFile.getRootElement().getContent();
            // and an iterator for the loop below
            Iterator<?> iterator = authorList.iterator();
            // counter for the return value if a found author matches the parameter
            int cnt = 1;
            // iterate all author values
            while (iterator.hasNext()) {
                Element author = (Element) iterator.next();
                // retrieve bibkey-attribute. since this attribute is optional,
                // "bk" also might be null!
                String bk = author.getAttributeValue(ATTRIBUTE_AUTHOR_BIBKEY);
                // if bibkey-attribute matches the parameter string, return the position
                if (bk!=null && bk.equals(bibkey)) {
                    return cnt;
                }
                // else increase counter
                cnt++;
            }
            // if no bibkey was found, return -1
            return -1;
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return -1;
        }
    }


    /**
     * This method adds a new keyword item to the keyword xml datafile
     * 
     * @param kw the keyword which should be added
     * @param freq the new frequency of the keyword, or - if keyword already exists, e.g. in case
     * of merging entries or adding existing keywords to an entry - the increasement-step of the
     * frequency-occurences of existing keywords. use "1" if a keyword is simply added to an entry, so
     * in case the keyword already exists, its frequency is increased by 1.
     * @return position of the recently added keyword, or -1 if keyword could not be added
     */
    public int addKeyword(String kw, int freq) {
        // check for valid value
        if (null==kw || kw.isEmpty()) {
            return -1;
        }
        // trim leading and trailing spaces
        kw = kw.trim();
        // if keyeord is empty, return
        if (kw.isEmpty()) {
            return -1;
        }
        // check whether author already exists
        int pos = getKeywordPosition(kw,false);
        // if keyword already exists, just increase counter
        if (pos!=-1) {
            try {
                // retrieve existing author
                Element keyw = retrieveElement(keywordFile, pos);
                // get the count-value, which indicates the frequency of occurences of this
                // keywords in the whole data file
                int f = Integer.parseInt(keyw.getAttributeValue(ATTRIBUTE_FREQUENCIES));
                // increase frequency by 1
                // change timestamp attribute
                updateKeywordTimestampAndID(keyw, f+freq, Tools.getTimeStampWithMilliseconds(), null);
                // change modified state
                setModified(true);
                // and return keyword index-number
                return pos;
            }
            catch (IllegalNameException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
        }
        // check whether we have any empty elements in between where we can insert the keyword
        int emptypos = retrieveFirstEmptyElement(keywordFile);
        // if we have any empty elements, go on here
        if (emptypos!=-1) {
            try {
                // retrieve empty element
                Element k = retrieveElement(keywordFile, emptypos);
                // set keyword string as new value
                k.setText(kw);
                // set frequency of occurences to 1
                // set timestamp attribute
                // set ID attribute
                // but first, check the length of "kw", because we want max. 5 first chars of kw
                // in keyword id
                String kwid;
                try {
                     kwid = kw.substring(0, 5);
                }
                catch (IndexOutOfBoundsException ex) {
                    kwid = kw;
                }
                updateKeywordTimestampAndID(k, freq, Tools.getTimeStampWithMilliseconds(), String.valueOf(emptypos)+kwid+Tools.getTimeStampWithMilliseconds());
                // change list-up-to-date-state
                setKeywordlistUpToDate(false);
                // change modified state
                setModified(true);
                // return the empty-position, which is now filled with the new keyword-value
                return emptypos;
            }
            catch (IllegalNameException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
        }
        // get the root element of the keyword xml datafile
        else {
            try {
                Element kwFile = keywordFile.getRootElement();
                // create a new keyword element
                Element newKeyword = new Element(ELEMENT_ENTRY);
                // add the new keyword element to the keyword datafile
                try {
                    kwFile.addContent(newKeyword);
                    // and finally add the parameter (new keyword string) to the recently created
                    // keyword element
                    newKeyword.addContent(kw);
                    // set frequency of occurences to 1
                    // set timestamp attribute
                    // set ID attribute
                    // but first, check the length of "kw", because we want max. 5 first chars of kw
                    // in keyword id
                    String kwid;
                    try {
                         kwid = kw.substring(0, 5);
                    }
                    catch (IndexOutOfBoundsException ex) {
                        kwid = kw;
                    }
                    updateKeywordTimestampAndID(newKeyword, freq, Tools.getTimeStampWithMilliseconds(), String.valueOf(keywordFile.getRootElement().getContent().size())+kwid+Tools.getTimeStampWithMilliseconds());
                    // change list-up-to-date-state
                    setKeywordlistUpToDate(false);
                    // change modified state
                    setModified(true);
                }
                catch (IllegalAddException e) {
                    // do nothing here
                    Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
                }
                catch (IllegalNameException ex) {
                    Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                }
                catch (IllegalDataException ex) {
                    Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                }
                // return the new size of the keyword file, i.e. the keyword position of 
                // the recently added keyword entry
                //
                // get a list with all entry-elements of the keyword data
                List<?> keywordList = keywordFile.getRootElement().getContent();
                // and return the size of this list
                return keywordList.size();
            }
            catch (IllegalStateException e) {
                Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
                return -1;
            }
        }
    }
    
    
    /**
     * This method sets or changes the frequency-attribute, the timestamp-attribute and the ID-attribute of
     * either author- or keyword-elements.
     * 
     * @param e The element, either an author-element (see {@link #authorFile}) or keyword-element (see {@link #keywordFile})
     * @param attr_f the string value of the frequencies attribute name, usually use {@link #ATTRIBUTE_FREQUENCIES} here.
     * @param attr_ts the string value of the timestamp attribute name, use either {@link #ATTRIBUTE_AUTHOR_TIMESTAMP} or {@link #ATTRIBUTE_KEYWORD_TIMESTAMP}
     * @param attr_id the string value of the ID attribute name, use either {@link #ATTRIBUTE_AUTHOR_ID} or {@link #ATTRIBUTE_KEYWORD_ID}
     * @param freq the new frequency-value of the frequency-attribute. Use {@code -1} if you don't want to change this attribute value.
     * @param ts the new timestamp as string. use {@code null} as parameter if you don't want to change the timestamp attribute.
     * @param id the new ID as string. use {@code null} as parameter if you don't want to change the ID attribute.
     */
    private void updateTimestampAndID(Element e, String attr_f, String attr_ts, String attr_id, int freq, String ts, String id) {
        // set frequency of occurences to 1
        if (freq!=-1) {
            e.setAttribute(attr_f, String.valueOf(freq));
        }
        // set timestamp attribute
        if (attr_ts!=null & !attr_ts.isEmpty() && ts!=null) {
            e.setAttribute(attr_ts, ts);
        }
        // set ID attribute
        if (attr_id!=null & !attr_id.isEmpty() && id!=null) {
            e.setAttribute(attr_id, id);
        }
    }
    /**
     * This method sets or changes the frequency-attribute, the timestamp-attribute and the ID-attribute of
     * author-elements.
     * 
     * @param e The author-element (see {@link #authorFile})
     * @param freq the new frequency-value of the frequency-attribute. Use {@code -1} if you don't want to change this attribute value.
     * @param ts the new timestamp as string. use {@code null} as parameter if you don't want to change the timestamp attribute.
     * @param id the new ID as string. use {@code null} as parameter if you don't want to change the ID attribute.
     */
    private void updateAuthorTimestampAndID(Element e, int freq, String ts, String id) {
        updateTimestampAndID(e, ATTRIBUTE_FREQUENCIES, ATTRIBUTE_AUTHOR_TIMESTAMP, ATTRIBUTE_AUTHOR_ID, freq, ts, id);
    }
    /**
     * This method sets or changes the frequency-attribute, the timestamp-attribute and the ID-attribute of
     * keyword-elements.
     * 
     * @param e The keyword-element (see {@link #keywordFile})
     * @param freq the new frequency-value of the frequency-attribute. Use {@code -1} if you don't want to change this attribute value.
     * @param ts the new timestamp as string. use {@code null} as parameter if you don't want to change the timestamp attribute.
     * @param id the new ID as string. use {@code null} as parameter if you don't want to change the ID attribute.
     */
    private void updateKeywordTimestampAndID(Element e, int freq, String ts, String id) {
        updateTimestampAndID(e, ATTRIBUTE_FREQUENCIES, ATTRIBUTE_KEYWORD_TIMESTAMP, ATTRIBUTE_KEYWORD_ID, freq, ts, id);
    }


    
    /**
     * This method adds several keywords to the keyword xml datafile, without assigning them
     * to a certain entry
     * 
     * @param kws the keywords which should be added
     */
    public void addKeywordsToDatabase(String[] kws) {
        // if keyeord is empty, return
        if (null==kws || 0==kws.length) {
            return;
        }
        // iterate all keywords
        for (String kw : kws) {
            // check whether keyword already exists
            int pos = getKeywordPosition(kw,false);
            // no, we have a new keyword. so add it...
            if (-1==pos) {
                // check whether we have any empty elements in between where we can insert the keyword
                int emptypos = retrieveFirstEmptyElement(keywordFile);
                // if we have any empty elements, go on here
                if (emptypos!=-1) {
                    try {
                        // retrieve empty element
                        Element k = retrieveElement(keywordFile, emptypos);
                        // set keyword string as new value
                        k.setText(kw);
                        // set frequency of occurences to 0
                        // set timestamp attribute
                        // set ID attribute
                        // but first, check the length of "kw", because we want max. 5 first chars of kw
                        // in keyword id
                        String kwid;
                        try {
                             kwid = kw.substring(0, 5);
                        }
                        catch (IndexOutOfBoundsException ex) {
                            kwid = kw;
                        }
                        updateKeywordTimestampAndID(k, 0, Tools.getTimeStampWithMilliseconds(), String.valueOf(emptypos)+kwid+Tools.getTimeStampWithMilliseconds());
                        // change list-up-to-date-state
                        setKeywordlistUpToDate(false);
                        // change modified state
                        setModified(true);
                    }
                    catch (IllegalNameException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalDataException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                }
                // get the root element of the keyword xml datafile
                else {
                    Element kwFile = keywordFile.getRootElement();
                    // create a new keyword element
                    Element newKeyword = new Element(ELEMENT_ENTRY);
                    // add the new keyword element to the keyword datafile
                    try {
                        kwFile.addContent(newKeyword);
                        // and finally add the parameter (new keyword string) to the recently created
                        // keyword element
                        newKeyword.addContent(kw);
                        // set frequency of occurences to 0
                        // set timestamp attribute
                        // set ID attribute
                        // but first, check the length of "kw", because we want max. 5 first chars of kw
                        // in keyword id
                        String kwid;
                        try {
                             kwid = kw.substring(0, 5);
                        }
                        catch (IndexOutOfBoundsException ex) {
                            kwid = kw;
                        }
                        updateKeywordTimestampAndID(newKeyword, 0, Tools.getTimeStampWithMilliseconds(), String.valueOf(keywordFile.getRootElement().getContent().size())+kwid+Tools.getTimeStampWithMilliseconds());
                        // change list-up-to-date-state
                        setKeywordlistUpToDate(false);
                        // change modified state
                        setModified(true);
                    }
                    catch (IllegalAddException e) {
                        // do nothing here
                        Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
                    }
                    catch (IllegalNameException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                    catch (IllegalDataException ex) {
                        Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                    }
                }
            }
        }
    }
    
    
    /**
     * This methods returns the keyword of a given position in the <b>keyword-datafile</b>.<br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(KWCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param pos a valid position of an element, ranged from 1 to {@link #getCount(int) getCount(KWCOUNT)}
     * @return the keyword string, or an empty string, if no such keyword exists
     */
    public String getKeyword(int pos) {
        // retrieve the keyword element
        Element keyword = retrieveElement(keywordFile, pos);
        // return the matching string value of the keyword element
        String retval;
        // check whether element is null
        if (null==keyword) {
            retval = "";
        }
        else {
            retval = keyword.getText();
        }
        
        return retval;
    }
    
    
    /**
     * This method sets a keyword to a given position in the keyword datafile
     * could be used for overwriting/changing existing keywords
     * 
     * @param pos the position of the keyword
     * @param kw the keyword string itself
     */
    public void setKeyword(int pos, String kw) {
        // retrieve and store the old keyword that should be replaced by the new one.
        // we need this to check whether we also have to replace synonyms...
        String oldkeyword = getKeyword(pos);
        // create a list of all keyword elements from the keyword xml file
        try {
            // retrieve keyword
            Element keyword = retrieveElement(keywordFile,pos);
            // if a valid element was found...
            if (keyword!=null) {
                // ...set the new text
                keyword.setText(kw);
                // find the oldkeyword in the synonymsfile...
                int synpos = synonymsObj.findSynonym(oldkeyword, true);
                // if we found a synonym, ask the user whether it also should be replaced
                if (synpos!=-1) {
                    // create a JOptionPane with yes/no/cancel options
                    int option = JOptionPane.showConfirmDialog(zknframe.getFrame(), zknframe.getResourceMap().getString("replaceKeywordsInSynonymsMsg",oldkeyword,kw), zknframe.getResourceMap().getString("replaceKeywordsInSynonymsTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                    // when the user applied to yes, we also change the synonym
                    if (JOptionPane.YES_OPTION == option) {
                        // get the synonymline
                        String[] synline = synonymsObj.getSynonymLine(synpos, true);
                        // go through all synonyms...
                        if (synline!=null && synline.length>1) {
                            for (int cnt=0; cnt<synline.length; cnt++) {
                                // ...and check whether the synonym-word equals the old keyword. if yes, replace
                                // the synonym at that position with the new keyword
                                if (synline[cnt].equals(oldkeyword)) {
                                    synline[cnt] = kw;
                                }
                            }                       
                        }
                        // finally, set back the synonyms.
                        synonymsObj.setSynonymLine(synpos, synline);
                    }
                }
                // and change the modified state of the file
                setModified(true);
            }
        }
        catch (IllegalStateException e) {
            // do nothing here
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
        }
    }
    
    
    /**
     * This method sets a keyword to a given position in the keyword datafile
     * could be used for overwriting/changing existing keywords.
     * <br><br>
     * This method is only used to update a data file from an older data
     * version, see CUpdateVersion for more details...
     *
     * @param pos the position of the keyword
     * @param kw the keyword string itself
     * @param freq the frequency of the keyword
     */
    public void setKeyword(int pos, String kw, int freq) {
        // create a list of all keyword elements from the keyword xml file
        try {
            // retrieve keyword
            Element keyword = retrieveElement(keywordFile,pos);
            // if a valid element was found...
            if (keyword!=null) {
                try {
                    // ...set the new text
                    keyword.setText(kw);
                    // and the frequency
                    keyword.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(freq));
                    // and change the modified state of the file
                    setModified(true);
                }
                catch (IllegalNameException ex) {
                    Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                }
                catch (IllegalDataException ex) {
                    Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                }
            }
        }
        catch (IllegalStateException e) {
            // do nothing here
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
        }
    }


    /**
     * This method searches the keyword-xml-file or author-xml-file for empty elements and returns the
     * number of the first empty element, if any. empty elements occur, when the user
     * deletes a keyword or author. in this case, to keep the permanent index-number of the other
     * keywords and authors, the keyword-/author-element is not completely removed, but only the text is
     * removed.
     * 
     * @param the xml-document (either <i>keywordFile</i> or <i>authorFile</i>)
     * @return the number of the first empty element, or -1 if no empty element was found
     */
    private int retrieveFirstEmptyElement(Document doc) {
        // create a list of all elements from the given xml file
        try { 
            List<?> elementList = doc.getRootElement().getContent();
            // and an iterator for the loop below
            Iterator<?> iterator = elementList.iterator();
            // counter for the return value if a found author matches the parameter
            int cnt = 1;
            // iterare all elements
            while (iterator.hasNext()) {
                Element el = (Element) iterator.next();
                // if author matches the parameter string, return the position
                if (el.getText().isEmpty()) {
                    return cnt;
                }
                // else increase counter
                cnt++;
            }
            // if no author was found, return -1
            return -1;
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return -1;
        }
    }

    
    /**
     * This method searches the keyword-xml-file or author-xml-file for empty elements and returns the
     * number of the first empty element, if any. empty elements occur, when the user
     * deletes a keyword or author. in this case, to keep the permanent index-number of the other
     * keywords and authors, the keyword-/author-element is not completely removed, but only the text is
     * removed.
     *
     * @param the xml-document (either <i>keywordFile</i> or <i>authorFile</i>)
     * @return the number of the first empty element, or -1 if no empty element was found
     */
    private int retrieveFirstEmptyEntry() {
        // create a list of all elements from the given xml file
        try {
            List<?> elementList = zknFile.getRootElement().getContent();
            // and an iterator for the loop below
            Iterator<?> iterator = elementList.iterator();
            // counter for the return value if a found author matches the parameter
            int cnt = 1;
            while (iterator.hasNext()) {
                Element el = (Element) iterator.next();
                // if author matches the parameter string, return the position
                if (el.getChild(ELEMENT_TITLE).getText().isEmpty() &&
                    el.getChild(ELEMENT_CONTENT).getText().isEmpty() &&
                    el.getChild(ELEMENT_AUTHOR).getText().isEmpty()) {
                    return cnt;
                }
                // else increase counter
                cnt++;
            }
            // if no author was found, return -1
            return -1;
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return -1;
        }
    }


    /**
     * This method deletes a keyword by removing the content from the element
     * inside of the keyword xml datafile. the element itself is kept and left
     * empty. this ensures that the order and numbering of a keyword never
     * changes. Since the zettelkasten datafile stores the index-numbers of the keywords
     * a changing in the position/order/numbering of the keyword datafile would lead
     * to corrupted keyword associations in the zettelkasten data file
     * 
     * @param pos (position of keyword which should be deleted)
     */
    public void deleteKeyword(int pos) {
        // check whether keyword exists...
        if (!getKeyword(pos).isEmpty()) {
            // ...delete its content
            // therefore, get the keyword's index-number as string (for comparison below)
            String nr = String.valueOf(pos);
            // create new string buffer
            StringBuilder newKw = new StringBuilder("");
            // and delete this index-number from all entries
            for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
                // get each element
                Element zettel = retrieveElement(zknFile,cnt);
                // get the keyword-index-numbers
                String[] kws = zettel.getChild(ELEMENT_KEYWORD).getText().split(",");
                // reset buffer
                newKw.setLength(0);
                for (String kw : kws) {
                    // if deleted value does not equal keyword-value, add it
                    if (!kw.equals(nr)) {
                        // append index-number
                        newKw.append(kw);
                        // and a seperator-comma
                        newKw.append(",");
                    }
                }
                // shorten the stringbuffer by one char, since we have a
                // superfluous comma char (see for-loop above)
                if (newKw.length()>0) {
                    newKw.setLength(newKw.length()-1);
                }
                // now set the new keyword-index-numbers to the zettel
                zettel.getChild(ELEMENT_KEYWORD).setText(newKw.toString());
            }
            // we don't want to remove the element itself, because this would lead
            // to changing index-numbers/element-position within the document. however,
            // a keyword should ever keep the same index-number. rather, we could fill
            // this "empty space" with new keywords,
            Element keyword = retrieveElement(keywordFile,pos);
            // if we have an element, go on
            if (keyword!=null) {
                // delete text
                keyword.setText("");
                // and reset attributes
                keyword.setAttribute(ATTRIBUTE_FREQUENCIES, "0");
                keyword.setAttribute(ATTRIBUTE_KEYWORD_ID, "");
                keyword.setAttribute(ATTRIBUTE_KEYWORD_TIMESTAMP, "");
            }
            // and change modified state
            setModified(true);
        }
    }


    /**
     * This method deletes an entry at thegiven position. since an entry-index-number
     * should never change to ensure that each entry always keeps its index-number,
     * we don't completely remove the element from the xml-file. rather, we simply delete
     * the content by setting empty values, so we have an "empty" element.
     * 
     * @param pos the position of the entry which should be deleted
     * @return {@code true} if entry was successfully deleted {@code false} if it could not be deleted
     * (because it already has been deleted before, or entry-element did not exist).
     */
    public boolean deleteZettel(int pos) {
        // check whether entry has already been deleted
        if (isDeleted(pos)) {
            // log error
            Constants.zknlogger.log(Level.WARNING, "Could not delete entry {0}! Entry already has been deleted!", String.valueOf(pos));
            return false;
        }
        // retrieve the entry-element at the given position
        Element zettel = retrieveElement(zknFile, pos);
        // if the entry-element exists...
        if (zettel!=null) {
            // remove this entry from the visible order
            // therefore, the previous entry of this entry should point
            // to the next entry of this entry
            setNextZettel(getPrevZettel(pos), getNextZettel(pos));
            // and the the next entry of this entry should point
            // to the previous entry of this entry
            setPrevZettel(getNextZettel(pos), getPrevZettel(pos));
            // check whether deleted entry was first entry
            if (pos==getFirstZettel()) {
                setFirstZettel(getNextZettel(pos));
            }
            // check whether deleted entry was last entry
            if (pos==getLastZettel()) {
                setLastZettel(getPrevZettel(pos));
            }
            // change zettelcounter
            zettelPos = getNextZettel(pos);
            // check whether it's out of bounds
            if (zettelPos>getCount(ZKNCOUNT) || zettelPos==-1) {
                zettelPos = getFirstZettel();
            }
            // change author-and keyword-frequencies
            changeFrequencies(pos,-1);
            // retrieve manual links, so we can delete the backlinks from other entries.
            // each manual link from this entry to other entries creates a "backlink" from
            // other entries to this one. if we delete the manual links from this entry,
            // all backlinks to this entry are removed.
            String[] manlinks = zettel.getChild(ELEMENT_MANLINKS).getText().split(",");
            // delete manual links
            deleteManualLinks(manlinks);
            // ...delete entry's attributes
            zettel.setAttribute(ATTRIBUTE_ZETTEL_ID, "");
            zettel.setAttribute(ATTRIBUTE_RATINGCOUNT,"");
            zettel.setAttribute(ATTRIBUTE_RATING,"");
            zettel.setAttribute(ATTRIBUTE_NEXT_ZETTEL, "");
            zettel.setAttribute(ATTRIBUTE_PREV_ZETTEL, "");
            // ...delete entry's content
            zettel.getChild(ELEMENT_TITLE).setText("");
            zettel.getChild(ELEMENT_CONTENT).setText("");
            zettel.getChild(ELEMENT_AUTHOR).setText("");
            zettel.getChild(ELEMENT_KEYWORD).setText("");
            zettel.getChild(ELEMENT_MANLINKS).setText("");
            zettel.getChild(ELEMENT_REMARKS).setText("");
            zettel.getChild(ELEMENT_TRAILS).setText("");
            zettel.getChild(ELEMENT_ATTACHMENTS).removeContent();
//            zettel.getChild(ELEMENT_LUHMANN_NUMBER).setText("");
            // remove timestamp by setting creation and last modification timestamp
            // to empty strings
            setTimestamp(zettel, "", "");
            // and change modified state
            setModified(true);
            // update title list
            setTitlelistUpToDate(false);
            // return success
            return true;
        }
        // log error
        Constants.zknlogger.log(Level.WARNING, "Could not delete entry {0}! XML-element is null!", String.valueOf(pos));
        return false;
    }

    
    /**
     * This method deletes certain authors from an entry's author-list. Therefore,
     * the to be deleted authors are passed as parameter. Then this method searches
     * the entry for occurences of these authors and deletes the index-numbers
     * of the to be deleted authors from the entry-author-indexnumbers.
     * 
     * @param aus the to be deleted author values as strings
     * @param nr the number of the entry which authors should be deleted
     */
    public void deleteAuthorsFromEntry(String[] aus, int nr) {
        // if we have any authors, go on...
        if (aus!=null && aus.length>0) {
            // get the entry's authors-index-numbers
            int[] entryaus = getAuthorIndexNumbers(nr);
            // check whether we have any author index numbers at all
            if ((entryaus!=null)&&(entryaus.length>0)) {
                // init string buffer
                StringBuilder newau = new StringBuilder("");
                // iterate array of entry-authors
                for (int cnt=0; cnt<entryaus.length; cnt++) {
                    // init found-indicator
                    boolean found = false;
                    for (String au : aus) {
                        // get author index-number
                        int pos = getAuthorPosition(au);
                        // if an author, that should be deleted, is found, set found indicator to true
                        if (pos==entryaus[cnt]) {
                            found=true;
                        }
                    }
                    // if no author, that should be deleted, was found...
                    if (!found) {
                        // append the author-index-number to the stringbuffer
                        newau.append(String.valueOf(entryaus[cnt]));
                        newau.append(",");
                    }
                }
                // finally, cut off the last comma that we don't need. we only
                // have this comma, if we added at least on keyword-indexnumber
                if (newau.length()>1) {
                    newau.setLength(newau.length()-1);
                }
                // set new index numbers
                setAuthorIndexNumbers(nr, newau.toString());
                // we don't need to change modified state here, since this is done
                // in the setAuthorIndexNumbers-method
                //
                // but we have to change the frequency of occurences of authors,
                // what we do now...
                for (String a : aus) {
                    // retrieve existing author
                    Element au = retrieveElement(authorFile, getAuthorPosition(a));
                    // get the count-value, which indicates the frequency of occurences of this
                    // author in the whole data file
                    String freq = au.getAttributeValue(ATTRIBUTE_FREQUENCIES);
                    if (freq!=null) {
                        int f = Integer.parseInt(freq);
                        // decrease frequency by 1
                        au.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(f-1));
                    }
                }
            }
        }
    }

    
    /**
     * This method deletes certain keywords from an entry's keyword-list. Therefore,
     * the to be deleted keywords are passed as parameter. Then this method searches
     * the entry for occurences of these keywords and deletes the index-numbers
     * of the to be deleted keywords from the entry-keyword-indexnumbers.
     * 
     * @param kws the to be deleted keyword values as strings
     * @param nr the number of the entry which keywords should be deleted
     */
    public void deleteKeywordsFromEntry(String[] kws, int nr) {
        // if we have any keywords, go on...
        if (kws!=null && kws.length>0) {
            // get the entry's keyword-index-numbers
            int[] entrykws = getKeywordIndexNumbers(nr);
            // check whether we have any keywords at all...
            if ((entrykws!=null)&&(entrykws.length>0)) {
                // init string buffer
                StringBuilder newkw = new StringBuilder("");
                // iterate array of entry-keywords
                for (int cnt=0; cnt<entrykws.length; cnt++) {
                    // init found-indicator
                    boolean found = false;
                    for (String kw : kws) {
                        // get keyword index-number
                        int pos = getKeywordPosition(kw, false);
                        // if a keyword, that should be deleted, is found, set found indicator to true
                        if (pos==entrykws[cnt]) {
                            found=true;
                        }
                    }
                    // if no keyword, that should be deleted, was found...
                    if (!found) {
                        // append the keyword-number to the stringbuffer
                        newkw.append(String.valueOf(entrykws[cnt]));
                        newkw.append(",");
                    }
                }
                // finally, cut off the last comma that we don't need. we only
                // have this comma, if we added at least on keyword-indexnumber
                if (newkw.length()>1) {
                    newkw.setLength(newkw.length()-1);
                }
                // set new index numbers
                setKeywordIndexNumbers(nr, newkw.toString());
                // we don't need to change modified state here, since this is done
                // in the setKeywordIndexNumbers-method
                //
                // but we have to change the frequency of occurences of keywords,
                // what we do now...
                for (String k : kws) {
                    // retrieve existing author
                    Element kw = retrieveElement(keywordFile, getKeywordPosition(k,false));
                    // get the count-value, which indicates the frequency of occurences of this
                    // author in the whole data file
                    int f = Integer.parseInt(kw.getAttributeValue(ATTRIBUTE_FREQUENCIES));
                    // decrease frequency by 1
                    kw.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(f-1));
                }
            }
        }
    }
    

    /**
     * This methods searches the entry {@code nr} and looks for occurences of all keywords that
     * have been passed in the array {@code kws}. All keywords of this array that already exist
     * in the entry {@code nr} will be removed.<br><br>
     * After that, the user is asked to replace keywords, which appear as synonyms, with their
     * related index-words (which is recommended, since some functions look for synonyms according
     * to a given index-word; thus, an existing keyword that has synonyms should always appear as
     * so called <i>index-word</i>).<br><br>
     * Finally, a "cleaned" array of keywords that do not already exist in the entry are returned...
     *
     * @param kws the keywords as string-array that should be checked for whether it already exists or not
     * @param nr the index-number of the entry we want to know from whether it contains the keyword
     * @param matchcase whether the check-for-keywords shoould be case sensitive (true) or not (false)
     * @return a cleaned array that contains all keywords that are new to the entry, or {@code null} if
     * the array {@code kws} did not contain any new keywords.
     */
    public String[] retrieveNonexistingKeywords(String[] kws, int nr, boolean matchcase) {
        // get the keyword-index-nunbers from the related entry
        int[] kwnr = getKeywordIndexNumbers(nr);
        // if we don't have any keywords, return the whole string-array with keywords which
        // was passed as parameter, since non of them can already exist in the current entry
        if ((null==kwnr)||(kwnr.length<1)) {
            // copy linked list to array and ask the user if he wants to replace possible synonym-keywords
            // with their related index-words...
            return Tools.replaceSynonymsWithKeywords(synonymsObj,kws);
        }
        // create linked list that will contain all new keywords that don't already
        // exist in the entry "nr"
        List<String> cleanedKeywords = new ArrayList<String>();
        // now check for the existence of each keyword in the entry "nr" and
        // add all non-existing (new) keywords to the linked list...
        for (String kw : kws) {
            if (!existsInKeywords(kw,nr,matchcase)) {
                cleanedKeywords.add(kw);
            }
        }
        // if we don't have any keywords left, return null...
        if (cleanedKeywords.size()<1) {
            return null;
        }
        // copy linked list to array and ask the user if he wants to replace possible synonym-keywords
        // with their related index-words...
        kws = Tools.replaceSynonymsWithKeywords(synonymsObj,cleanedKeywords.toArray(new String[cleanedKeywords.size()]));
        // due to the replacement of keywords by their index-words we can again have double keywords now.
        // so we check for existing keywords in the new returned keyword-array again...
        if (kws!=null && kws.length>0) {
            // clear linked list
            cleanedKeywords.clear();
            // check for the existence of each keyword in the entry "nr" and
            // add all non-existing (new) keywords to the linked list...
            for (String kw : kws) {
                if (!existsInKeywords(kw,nr,matchcase)) {
                    cleanedKeywords.add(kw);
                }
            }
            // if we don't have any keywords left, return null...
            if (cleanedKeywords.size()<1) {
                return null;
            }
            // else create new return-array
            kws = cleanedKeywords.toArray(new String[cleanedKeywords.size()]);
        }
        // return "cleaned" keyword-array. this array now contains only those keywords that are new to the entry
        // and which are not synonyms, but the related index-words...
        return kws;
    }


    /**
     * This methods searches the entry {@code nr} and looks for occurences of <b>all</b> the keyword
     * in the array {@code kws}.
     * If the entry contains all keywords of {@code kws}, this method returns true, i.e. the method
     * found out that the requested keyword already exists in the entry's keyword-list.<br><br>
     * <b>Attention!</b> In case you need to know whether a keyword exists in the <i>keyword.xml</i>-file
     * (i.e. the keywords-data-file), use the method {@link #getKeywordPosition(java.lang.String, boolean) getKeywordPosition()}
     * insted.
     * 
     * @param kws the keywords which should be checked for whether they already exists or not
     * @param nr the index-number of the entry we want to know from whether it contains the keyword
     * @param log_and whether we have logical-and-search (<i>all</i> keywords of "kws" must exist
     * in that entry) or logical-or-search (<i>at least one</i> keyword of "kws" exists in entry.
     * @param matchcase whether the checking for keywords should be case-sensitive (true) or not (false)
     * @return {@code true} if the entry contains <b>all</b> keywords when we have logical-and-search, or
     * true when the entry contains <b>at least one</b> keyword when we have logical-or-search.
     * false otherwise
     */
    public boolean existsInKeywords(String[] kws, int nr, boolean log_and, boolean matchcase) {
        // get the keyword-index-nunbers from the related entry
        int[] kwnr = getKeywordIndexNumbers(nr);
        // if we don't have any keywords, return false
        if ((null==kwnr)||(kwnr.length<1)) {
            return false;
        }
        // a counter which indicates the amount of occurences
        int foundcount = 0;
        for (String kw : kws) {
            // reset found value
            boolean found = false;
            // get the keyword position (i.e. the index-number) of the passed parameter-string
            int pos = getKeywordPosition(kw, matchcase);
            // iterate the array of keyword-index-numbers of the target-entry
            // if the keyword we are looking for already exists, set found-value to true
            for (int loop : kwnr) {
                if (loop==pos) {
                    found = true;
                }
            }
            // if we have found the keyword...
            if (found) {
                // either increase found-counter, when we have logical-and-combination
                if (log_and) {
                    foundcount++;
                }
                // or simply return true when we have logical-or-search
                else {
                    return true;
                }
            }
        }
        // if we found as much keywords as we have in the array, the
        // return-result should be true...
        return (foundcount==kws.length);
    }
    
    
    /**
     * This methods searches the entry {@code nr} and looks for occurences of the keyword {@code kw}.
     * If the entry contains the keyword {@code kw}, this method returns true, i.e. the method
     * found out that the requested keyword already exists in the entry's keyword-list.<br><br>
     * <b>Attention!</b> In case you need to know whether a keyword exists in the <i>keyword.xml</i>-file
     * (i.e. the keywords-data-file), use the method {@link #getKeywordPosition(java.lang.String, boolean) getKeywordPosition()}
     * instead.
     * 
     * @param kw the keyword which should be checked for whether it already exists or not
     * @param nr the index-number of the entry we want to know from whether it contains the keyword
     * @param matchcase whether the check-for-keywords shoould be case sensitive (true) or not (false)
     * @return {@code true} if keyword already exists in the entry <i>nr</i>, false otherwise
     */
    public boolean existsInKeywords(String kw, int nr, boolean matchcase) {
        // get the keyword-index-nunbers from the related entry
        int[] kwnr = getKeywordIndexNumbers(nr);
        // if we don't have any keywords, return false
        if ((null==kwnr)||(kwnr.length<1)) {
            return false;
        }
        // get the keyword position (i.e. the index-number) of the passed parameter-string
        int pos = getKeywordPosition(kw,matchcase);
        // prepare return value
        boolean retval = false;
        // iterate the array of keyword-index-numbers of the target-entry
        // if the keyword we are looking for already exists, set return value to true
        for (int loop : kwnr) {
            if (loop==pos) {
                retval = true;
            }
        }
        return retval;
    }
    
    
    /**
     * This methods searches the entry {@code nr} and looks for occurences of the author {@code au}.
     * If the entry contains the author {@code au}, this method returns true, i.e. the method
     * found out that the requested author already exists in the entry's author-list.<br><br>
     * <b>Attention!</b> In case you need to know whether an author exists in the <i>author.xml</i>-file
     * (i.e. the authors-data-file), use the method {@link #getAuthorPosition(java.lang.String) getAuthorPosition()}
     * instead.
     * 
     * @param au (the author which should be checked for whether it already exists or not)
     * @param nr (the index-number of the entry we want to know from whether it contains the author)
     * @return (true if author already exists, false otherwise)
     */
    public boolean existsInAuthors(String au, int nr) {
        return existsInAuthors(getAuthorPosition(au), nr);
    }
    
    
    /**
     * This methods searches the entry {@code nr} and looks for occurences of the author {@code au}.
     * If the entry contains the author {@code au}, this method returns true, i.e. the method
     * found out that the requested author already exists in the entry's author-list.<br><br>
     * <b>Attention!</b> In case you need to know whether an author exists in the <i>author.xml</i>-file
     * (i.e. the authors-data-file), use the method {@link #getAuthorPosition(java.lang.String) getAuthorPosition()}
     * instead.
     *
     * @param authorindexnumber the author-index-number which should be checked for whether it already exists or not
     * @param nr the index-number of the entry we want to know from whether it contains the author
     * @return {@code true} if author already exists, {@code false} otherwise
     */
    public boolean existsInAuthors(int authorindexnumber, int nr) {
        // get the author-index-nunbers from the related entry
        int[] aunr = getAuthorIndexNumbers(nr);
        // check whether we have any author index numbers at all
        if ((null==aunr)||(aunr.length<1)) {
            return false;
        }
        // prepare return value
        boolean retval = false;
        // iterate the array of author-index-numbers of the target-entry
        // if the author-index-number we are looking for already exists, set return value to true
        for (int loop : aunr) {
            if (loop==authorindexnumber) {
                retval = true;
            }
        }
        return retval;
    }
    

    /**
     * This method merges two keywords. The method searches for the keyword "oldkw" in all entries.
     * If this keyword was found in an entry, the method then looks for the new keyword "newkw" in
     * that entry. If the new keyword also exists, nothing is changed. If the new keyword does not
     * exist, it is added to the entry. at the end, the old keyword is deleted from the datafile via
     * the method "deleteKeyword()"
     * 
     * @param oldkw (the old keyword, which should be deleted)
     * @param newkw
     */
    public void mergeKeywords(String oldkw, String newkw) {
        // get the position (i.e. index-number) of the old (to be deleted) keyword
        int oldpos = getKeywordPosition(oldkw,false);
        // get the position (i.e. index-number) of the new keyword
        // (into which the old keyword should be transformed)
        int newpos = getKeywordPosition(newkw,false);
        // check whether both exist
        if ((oldpos!=-1) && (newpos!=-1)) {
            // now go through the whole dataset
            for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
                // check, which of each entry contains the old keyword
                // now we have to add the new keyword-index-number,
                // if it does not already exist. we don't need to delete the *old*
                // index-number, because at the end we simply delete it with the
                // "deleteKeyword()" method.
                // if the new keyword doesn't exist, add it
                if (existsInKeywords(oldkw,cnt,false) && !existsInKeywords(newkw,cnt,false)) {
                    addKeywordToEntry(newkw,cnt,1);
                }
            }
            // finally, delete old keyword
            deleteKeyword(oldpos);
        }
    }
    
    
    /**
     * This method merges two authors. The method searches for the author "oldau" in all entries.
     * If this author was found in an entry, the method then looks for the new author "newau" in
     * that entry. If the new author also exists, nothing is changed. If the new author does not
     * exist, it is added to the entry. at the end, the old author is deleted from the datafile via
     * the method "deleteAuthor()"
     * 
     * @param oldau (the old author, which should be deleted)
     * @param newau
     */
    public void mergeAuthors(String oldau, String newau) {
        // get the position (i.e. index-number) of the old (to be deleted) author
        int oldpos = getAuthorPosition(oldau);
        // get the position (i.e. index-number) of the new author
        // (into which the old author should be transformed)
        int newpos = getAuthorPosition(newau);
        // check whether both exist
        if ((oldpos!=-1) && (newpos!=-1)) {
            // now go through the whole dataset
            for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
                // check, which of each entry contains the old author
                // now we have to add the new author-index-number,
                // if it does not already exist. we don't need to delete the *old*
                // index-number, because at the end we simply delete it with the
                // "deleteAuthor()" method.
                // if the new author doesn't exist, add it
                if (existsInAuthors(oldau,cnt) && !existsInAuthors(newau,cnt)) {
                    addAuthorToEntry(newau,cnt,1);
                }
            }
            // finally, delete old author
            deleteAuthor(oldpos);
        }
    }
    
    
    /**
     * This method adds a keyword, which is passed as string-parameter, to an existing
     * entry. the method first checks whether the entry {@code nr} already contains the keyword
     * {@code kw}. if not, the index-number of that keyword is retrieved and the new index-numbers
     * are added to the entry.
     * 
     * @param kw the keyword which should be added to the entry
     * @param nr the index-number of the entry where the keyword should be added to
     * @param freq the new frequency of the keyword, or - if keyword already exists, e.g. in case
     * of merging entries or adding existing keywords to an entry - the increasement-step of the
     * frequency-occurences of existing keywords. use "1" if a keyword is simply added to an entry, so
     * in case the keyword already exists, its frequency is increased by 1.
     */
    public void addKeywordToEntry(String kw, int nr, int freq) {
        // trim leading and trailing spaces
        kw = kw.trim();
        // if keyword is empty, return
        if (kw.isEmpty()) {
            return;
        }
        // first check whether keyword already exists
        // return false, if keyword exists and we don't add it
        if (existsInKeywords(kw,nr,false)) {
            return;
        }
        // retrieve the current entry
        Element el = retrieveElement(zknFile,nr);
        // if we don't have a valid element, return false
        if (null==el || null==el.getChild(ELEMENT_KEYWORD)) {
            return;
        }
        // create empty stringbuffer
        StringBuilder sb = new StringBuilder("");
        // append keywords
        sb.append(el.getChild(ELEMENT_KEYWORD).getText());
        // append new separator, but only if we already have keywords
        if (sb.length()>0) {
            sb.append(",");
        }
        // add it to the keyword-list
        int pos = addKeyword(kw, freq);
        // only proceed when success
        if (pos!=-1) {
            // append index-number of the keyword which should be added
            sb.append(String.valueOf(pos));
            // set the new keyword-index-numbers
            el.getChild(ELEMENT_KEYWORD).setText(sb.toString());
            // finally, change modified state
            setModified(true);
        }
    }
    
    
    /**
     * This method adds several keywords, that are passed as string-array, to an existing
     * entry. the method first checks whether the entry {@code nr} already contains one of the
     * keywords given in the array {@code kws}.
     * if not, the index-number of that keyword is retrieved and the new index-numbers
     * are added to the entry.
     *
     * @param kws a string-array with keywords that should be added to the entry
     * @param nr the index-number of the entry where the keywords should be added to
     * @param freq the new frequency of the keywords, or - if any one of the keywords inside the array
     * {@code kws} already exists, e.g. in case
     * of merging entries or adding existing keywords to an entry - the increasement-step of the
     * frequency-occurences of existing keywords. use "1" if a keyword is simply added to an entry, so
     * in case the keyword already exists, its frequency is increased by 1.
     * @return 
     */
    public String[] addKeywordsToEntry(String[] kws, int nr, int freq) {
        // check for valid parameter. if not existing, return
        if (null==kws||kws.length<1) return null;
        // clean keywords, i.e. retrieve only those keywords that are new
        // to the entry....
        kws = retrieveNonexistingKeywords(kws,nr,false);
        // if we have any new keywords, go on here.
        if (kws!=null && kws.length>0) {
            // retrieve the current entry
            Element el = retrieveElement(zknFile,nr);
            // if we don't have a valid element, return false
            if (null==el || null==el.getChild(ELEMENT_KEYWORD)) return null;
            // create empty stringbuffer
            StringBuilder sb = new StringBuilder("");
            // append keywords
            sb.append(el.getChild(ELEMENT_KEYWORD).getText());
            // append new separator, but only if we already have keywords
            if (sb.length()>0) sb.append(",");
            // go through all keywords...
            for (String kw : kws) {
                // trim leading and trailing spaces
                kw = kw.trim();
                // if keyword is not empty and does not exist, go on
                if (!kw.isEmpty() && !existsInKeywords(kw, nr, false)) {
                    // add it to the keyword-list
                    int pos = addKeyword(kw, freq);
                    // append index-number of the keyword which should be added
                    if (pos!=-1) sb.append(String.valueOf(pos)).append(",");
                }
            }
            // delete last comma
            if (sb.length()>1) sb.setLength(sb.length()-1);
            // set the new keyword-index-numbers
            el.getChild(ELEMENT_KEYWORD).setText(sb.toString());
            // finally, change modified state
            setModified(true);
        }
        return kws;
    }


    /**
     * This method adds an author, which is passed as string-parameter, to an existing
     * entry. the method first checks whether the entry "nr" already contains the author
     * "au". if not, the index-number of that author is retrieved and the new index-numbers
     * are added to the entry.
     * 
     * @param au the author which should be added to the entry
     * @param nr the index-number of the entry where the keyword should be added to
     * @param freq the new frequency of the author, or - if author already exists, e.g. in case
     * of merging entries or adding existing authors to an entry - the increasement-step of the
     * frequency-occurences of existing authors. use "1" if an author is simply added to an entry, so
     * in case the author already exists, its frequency is increased by 1.
     */
    public void addAuthorToEntry(String au, int nr, int freq) {
        // trim leading and trailing spaces
        au = au.trim();
        // if author is empty, return
        if (au.isEmpty()) return;
        // first check whether author already exists in that entry
        // return false, if author exists and we don't add it
        if (existsInAuthors(au,nr)) return;
        // retrieve the current entry
        Element el = retrieveElement(zknFile,nr);
        // if we don't have a valid element, return false
        if (null==el || null==el.getChild(ELEMENT_AUTHOR)) return;
        // create empty stringbuffer
        StringBuilder sb = new StringBuilder("");
        // append author
        sb.append(el.getChild(ELEMENT_AUTHOR).getText());
        // append new separator, but only if we already have authors
        if (sb.length()>0) sb.append(",");
        // add it to the author-list
        int pos = addAuthor(au,freq);
        // only proceed when valid value
        if (pos!=-1) {
            // append index-number of the author which should be added
            sb.append(String.valueOf(pos));
            // set the new author-index-numbers
            el.getChild(ELEMENT_AUTHOR).setText(sb.toString());
            // finally, change modified state
            setModified(true);
        }
    }


    /**
     * This method returns the position of an author in the author XML file {@link #authorFile}.
     * if the author doesn't exist, the return value is {@code -1}.
     *
     * @param auth author which is searched for in the author list
     * @return the position of the author string or -1 if no match was found
     */
    public int findAuthorInDatabase(String auth) {
        return getAuthorPosition(auth);
    }

    
    /**
     * This method returns the position of an author in the author XML file {@link #authorFile}.
     * if the author doesn't exist, the return value is {@code -1}.
     * 
     * @param auth author which is searched for in the author list
     * @return the position of the author string or -1 if no match was found
     */
    public int getAuthorPosition(String auth) {
        // check for valid value
        if (null==auth || auth.trim().isEmpty()) {
            return -1;
        }
        // create a list of all author elements from the author xml file
        try {
            List<?> authorList = authorFile.getRootElement().getContent();
            // and an iterator for the loop below
            Iterator<?> iterator = authorList.iterator();
            // counter for the return value if a found author matches the parameter
            int cnt = 1;
            // iterate all author values
            while (iterator.hasNext()) {
                Element author = (Element) iterator.next();
                // if author matches the parameter string, return the position
                if (auth.equalsIgnoreCase(author.getText())) return cnt;
                // else increase counter
                cnt++;
            }
            // if no author was found, return -1
            return -1;
        }
        catch (IllegalStateException e) {
            return -1;
        }
    }

    
    /**
     * This method returns the position of an author in the author XML file,
     * which has the BibKey {@code bibkey} associated.
     * If no author with such BibKey exist, the return value is -1
     *
     * @param bibkey the bibkey which is searched for in the author list
     * @return the position of the author string that contains that {@code bibkey},
     * or -1 if no match was found
     */
    public int getAuthorBibKeyPosition(String bibkey) {
        // check for valid parameter
        if (null==bibkey || bibkey.isEmpty()) return -1;
        // iterate all authors
        for (int cnt=1; cnt<=getCount(AUCOUNT);cnt++) {
            // retrieve each author bibkey
            String aubib = getAuthorBibKey(cnt);
            // if author-value has a bibkey and this bibkey equals the bibkey-parameter,
            // then return the author-position
            if (aubib!=null && aubib.equalsIgnoreCase(bibkey)) return cnt;
        }
        // nothing found, so return -1
        return -1;
    }


    /**
     * This method adds a new author item to the author xml datafile
     * @param auth the author which should be added
     * @param freq the new frequency of the author, or - if author already exists, e.g. in case
     * of merging entries or adding existing authors to an entry - the increasement-step of the
     * frequency-occurences of existing authors. use "1" if an author is simply added to an entry, so
     * in case the author already exists, its frequency is increased by 1.
     * @return position of the recently added author, or -1 if author could not be added
     */
    public int addAuthor(String auth, int freq) {
        // trim leading and trailing spaces
        auth = auth.trim();
        // if author is empty, return
        if (auth.isEmpty()) return -1;
        // check whether author already exists
        int pos = getAuthorPosition(auth);
        // if author already exists, just increase counter
        if (pos!=-1) {
            try {
                // retrieve existing author
                Element au = retrieveElement(authorFile, pos);
                // get the count-value, which indicates the frequency of occurences of this
                // author in the whole data file
                int f = Integer.parseInt(au.getAttributeValue(ATTRIBUTE_FREQUENCIES));
                // increase frequency of occurences
                // change timestamp attribute
                updateAuthorTimestampAndID(au, f+freq, Tools.getTimeStampWithMilliseconds(), null);
                // change modified state
                setModified(true);
                // and return author index-number
                return pos;
            }
            catch (IllegalNameException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
        }
        // check whether we have any empty elements in between where we can insert the author
        int emptypos = retrieveFirstEmptyElement(authorFile);
        // if we have any empty elements, go on here
        if (emptypos!=-1) {
            try {
                // retrieve empty element
                Element au = retrieveElement(authorFile, emptypos);
                // set author string as new value
                au.setText(auth);
                // set frequency of occurences to 1
                // set timestamp attribute
                // set ID attribute
                // but first, check the length of "auth", because we want max. 5 first chars of auth
                // in author id
                String auid;
                try {
                     auid = auth.substring(0, 5);
                }
                catch (IndexOutOfBoundsException ex) {
                    auid = auth;
                }                
                updateAuthorTimestampAndID(au, freq, Tools.getTimeStampWithMilliseconds(), String.valueOf(emptypos)+auid+Tools.getTimeStampWithMilliseconds());
                // change list-up-to-date-state
                setAuthorlistUpToDate(false);
                // change modified state
                setModified(true);
                // return the empty-position, which is now filled with the new author-value
                return emptypos;
            }
            catch (IllegalNameException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
                return -1;
            }
        }
        // get the root element of the author xml datafile
        else try {
            // get the root element of the author xml datafile
            Element authFile = authorFile.getRootElement();
            // create a new author element
            Element newAuthor = new Element(ELEMENT_ENTRY);
            // add the new author element to the author datafile
            try {
                // add the new author element to the author datafile
                authFile.addContent(newAuthor);
                // and finally add the parameter (new author string) to the recently created
                // author element
                newAuthor.addContent(auth);
                // set frequency of occurences to 1
                // set timestamp attribute
                // set ID attribute
                // but first, check the length of "auth", because we want max. 5 first chars of auth
                // in author id
                String auid;
                try {
                     auid = auth.substring(0, 5);
                }
                catch (IndexOutOfBoundsException ex) {
                    auid = auth;
                }                
                updateAuthorTimestampAndID(newAuthor, freq, Tools.getTimeStampWithMilliseconds(), String.valueOf(authorFile.getRootElement().getContent().size())+auid+Tools.getTimeStampWithMilliseconds());
                // change list-up-to-date-state
                setAuthorlistUpToDate(false);
                // change modified state
                setModified(true);
            }
            catch (IllegalAddException ex) {
                Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
            }
            catch (IllegalNameException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            }
            // return the new size of the author file, i.e. the author position of 
            // the recently added author entry
            // get a list with all entry-elements of the author data
            List<?> authorList = authorFile.getRootElement().getContent();
            // and return the size of this list
            return authorList.size();
        }
        catch (IllegalStateException e) {
            Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
            return -1;
        }
    }


    /**
     * This method adds a new entry to the datafile. The needed parameters come from the JDialog
     * "CNewEntry.java". This dialog opens an edit-mask so the user can input the necessary information.
     * If everything is done, the JDialog retrieves all the information as string(-array)-variables
     * and simply passes these as paramaters to this method.
     * <br>
     * <br>
     * What we have to do here is to check whether the keywords or links e.g. partly exist, and if so,
     * find out the related index number. Keywords which until now do not already exist in the keyword
     * file have to be added to the keyword file and the new index number has to be addes to the
     * keyword-element of the entry. and so on...
     *
     * @param title the entry's title as string
     * @param content the entry's content as string
     * @param authors the entry's author as string, retrieve index number and add it to the entry's author-element.
     * use {@code null} if no authors should be added
     * @param keywords the entry's keywords as string-array. retrieve index numbers and add those to the entry's keyword-element
     * use {@code null} if no authors should be added
     * @param remarks the remarks as string
     * @param links the entry's links as string
     * use {@code null} if no authors should be added
     * @param timestamp the current date. in this case, add it as creation date to the timestamp
     * @param luhmann the number of the currently display entry, before the user clicked "new" or "insert entry".
     * if we have to insert an entry, we need to know this number, because that entry retrieves this new entry's
     * index-number and adds it to its luhmann-tag (which indicates follower- and sub-entries).
     * use {@code -1} if no luhmann-number is needed (i.e. no follower-entry is added).
     * @param editDeletedEntry use {@code true} if user edits an deleted entry, which is the same as inserting a
     * new entry at the deleted entry's position. use {@code false} if a entry is added normally.
     * @param editDeletedEntryPosition the position of the currently displayed entry that is deleted and should be
     * overwritten with a new entry ({@code editDeletedEntry} is set to true).
     * @param insertAfterEntry indicates the position, after which existing entry the new added entry should be inserted.
     * Use {@code -1} to add the new entry to the end of the database.
     * @return one of the following constants:<br>
     * {@link #ADD_ENTRY_OK ADD_ENTRY_OK} if a normal entry was successfully added<br>
     * {@link #ADD_LUHMANNENTRY_OK ADD_LUHMANNENTRY_OK} if a follower-entry (trailing entry) was successfully added<br>
     * {@link #ADD_ENTRY_ERR ADD_ENTRY_ERR} if an error occured when adding a normal entry<br>
     * {@link #ADD_LUHMANNENTRY_ERR ADD_LUHMANNENTRY_ERR} if an error occured when adding a follower-entry (trailing entry)
     */
    public int addEntry(String title, String content, String[] authors, String[] keywords, String remarks, String[] links, String timestamp, int luhmann, boolean editDeletedEntry, int editDeletedEntryPosition, int insertAfterEntry) {
        // init return value
        int retval = ADD_ENTRY_OK;
        List<Integer> manlinks;
        // check for valid content. if we have any content,
        // replace Unicode-chars with UBB-tags
        if (content!=null && !content.isEmpty()) {
            content = Tools.replaceUnicodeToUbb(content);
        }
        // create a new zettel-element
        Element zettel = new Element(ELEMENT_ZETTEL);
        // check whether we have any empty elements in between where we can insert the new entry
        int emptypos = (editDeletedEntry) ? editDeletedEntryPosition : retrieveFirstEmptyEntry();
        // check whether user wants to edit an already deleted entry and insert a new one at
        // that position
        if (editDeletedEntry || (emptypos!=-1 && settings.getInsertNewEntryAtEmpty())) {
            // retrieve empty element
            zettel = retrieveElement(zknFile, emptypos);
            // and remove former content, so we can add new content
            zettel.removeContent();
        }
        try {
            // add unique ID
            setZettelID(zettel);
            //
            // add title
            //
            // create child element with title information
            Element t = new Element(ELEMENT_TITLE);
            // and add it to the zettel-element
            zettel.addContent(t);
            // set value of the child element
            t.setText(title);
            //
            // add content
            //
            // create child element with content information
            Element c = new Element(ELEMENT_CONTENT);
            // and add it to the zettel-element
            zettel.addContent(c);
            // set value of the content element
            c.setText(content);
            // then, create form-images
            createFormImagesFromContent(content);
            //
            // add author
            //
            // create child element with author information
            Element a = new Element(ELEMENT_AUTHOR);
            // and add it to the zettel-element
            zettel.addContent(a);
            // create empty string buffer which stores the index numbers
            // of the converted authors
            StringBuilder newau = new StringBuilder("");
            // check whether we have authors at all
            if ((authors!=null) && (authors.length>0)) {
                // iterate the array and get the index number of each author string
                // if a author does not already exist, add it to the authorfile
                for (String aut : authors) {
                    // trim leading and trailing spaces
                    aut = aut.trim();
                    // only proceed for this entry, if it contains a value
                    if (!aut.isEmpty()) {
                        // add author
                        int authorPos = addAuthor(aut,1);
                        // append the index number in the string buffer
                        newau.append(String.valueOf(authorPos));
                        // separator for the the index numbers, since more authors
                        // and thus more index numbers might be stored in the author element
                        newau.append(",");
                    }
                }
                // check whether we have any author-value at all...
                if (newau.length()>0) {
                    // shorten the stringbuffer by one char, since we have a
                    // superfluous comma char (see for-loop above)
                    newau.setLength(newau.length()-1);
                    // and say that author list is out of date
                    setAuthorlistUpToDate(false);
                }
            }
            a.setText(newau.toString());
            //
            // add keywords
            //
            // create child element with keyword information
            Element k = new Element(ELEMENT_KEYWORD);
            // and add it to the zettel-element
            zettel.addContent(k);
            // create empty string buffer which stores the index numbers
            // of the converted keywords
            StringBuilder newkw = new StringBuilder("");
            // check whether we have keywords at all
            if ((keywords!=null) && (keywords.length>0)) {
                // iterate the array and get the index number of each keyword string
                // if a keyword does not already exist, add it to the keywordfile
                for (String keyw : keywords) {
                    // trim leading and trailing spaces
                    keyw = keyw.trim();
                    // only proceed for this entry, if it contains a value
                    if (!keyw.isEmpty()) {
                        // add it to the data file
                        // and store the position of the new added keyword in the
                        // variable keywordPos
                        int keywordPos = addKeyword(keyw,1);
                        // append the index number in the string buffer
                        newkw.append(String.valueOf(keywordPos));
                        // separator for the the index numbers, since more keywords
                        // and thus more index numbers might be stored in the keyword element
                        newkw.append(",");
                    }
                }
                // check whether we have any keyword-values at all...
                if (newkw.length()>0) {
                    // shorten the stringbuffer by one char, since we have a
                    // superfluous comma char (see for-loop above)
                    newkw.setLength(newkw.length()-1);
                    // and say that author list is out of date
                    setKeywordlistUpToDate(false);
                }
            }
            // store keyword index numbers
            k.setText(newkw.toString());
            //
            // now comes the manual links to other entries
            //
            Element m = new Element(ELEMENT_MANLINKS);
            zettel.addContent(m);
            // check for manual links in content
            // and add them
            manlinks = extractManualLinksFromContent(content);
            m.setText(retrievePreparedManualLinksFromContent(manlinks));
            //
            // add hyperlinks
            //
            // create child element with link information
            Element h = new Element(ELEMENT_ATTACHMENTS);
            // and add it to the zettel-element
            zettel.addContent(h);
            // add each hyperlink string
            if (links!=null && links.length>0) {
                // therefor, iterate the array
                for (String l : links) {
                    // create a new subchuld-element
                    Element sublink = new Element(ELEMENT_ATTCHILD);
                    // and add the link-string from the array
                    sublink.setText(l);
                    h.addContent(sublink);
                }
            }
            //
            // add remarks
            //
            // create child element with content information
            Element r = new Element(ELEMENT_REMARKS);
            // and add it to the zettel-element
            zettel.addContent(r);
            // set value of the content element
            r.setText(remarks);
            //
            // add remarks
            //
            // set creation timestamp, but set no text for edit timestamp
            // since the entry is not edited
            setTimestamp(zettel, Tools.getTimeStamp(), "");
            //
            // now comes the luhmann number
            //
            Element l = new Element(ELEMENT_TRAILS);
            zettel.addContent(l);
            l.setText("");
            //
            // complete datafile
            //
            // if we have any empty elements, go on here
            if (emptypos!=-1 && settings.getInsertNewEntryAtEmpty()) {
                // return the empty-position, which is now filled with the new author-value
                zettelPos = emptypos;
            }
            else {
                // finally, add the whole element to the data file
                zknFile.getRootElement().addContent(zettel);
                // set the zettel-position to the new entry
                zettelPos=getCount(ZKNCOUNT);
            }
            // and add the new position to the history...
            addToHistory();
            // set modified state
            setModified(true);
        }
        catch (IllegalAddException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return ADD_ENTRY_ERR;
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return ADD_ENTRY_ERR;
        }
        // if we have a follower-number (insert-entry), we have to change the luhmann-tag
        // of the related entry (which number is passed in the luhmann-variable)
        if (luhmann!=-1) {
            // try to add luhmann number
            if (addLuhmannNumber(luhmann, zettelPos)) {
                // if it was successfull, we can insert this entry
                // after the "parent" entry
                retval = ADD_LUHMANNENTRY_OK;
                // to do this, we need to change the "insertAfter" value
                insertAfterEntry = luhmann;
            }
            else {
                retval = ADD_LUHMANNENTRY_ERR;
            }
        }
        // check whether inserted entry position is already the last position in
        // the entry order
        // in this case, we can set the variable to -1, so it will automatically be
        // added to the end
        changeZettelPointer(zettelPos, insertAfterEntry);
        // set this entry as first entry if we do not have any
        // first entry yet...
        if (-1==getFirstZettel()) setFirstZettel(zettelPos);
        // save ID of last added entry
        setLastAddedZettelID(zettel);
        // create back-references for manual links
        // we can do this here first, because we need
        // "zettelPos" as reference, which is not available earlier
        addManualLink(manlinks, zettelPos);
        // entry successfully added
        return retval;
    }

    
    /**
     * This method updates the "pointer" references from entries when a new
     * entry is added to the data base. In this case, the former last entry
     * now refers to this entry as last entry, while the first entry back-references
     * to the new added entry (and no longer to the former last entry). The same
     * applies when an entry is inserted in between two entries.
     * <br><br>
     * This method is used for instance in the
     * {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int, int) addEntry}
     * or {@link #duplicateEntry(int) duplicateEntry} methods.
     * 
     * @param zettelPos the index-number of the entry that was added to the data base.
     * @param insertAfterEntry the position of the existing entry after which the new
     * entry should be added. use {@code -1} to add an entry to the end of the entry
     * order.
     */
    private void changeZettelPointer(int zettelPos, int insertAfterEntry) {
        // check whether entry should be inserted at the end of the database
        if (insertAfterEntry==getLastZettel()) insertAfterEntry = -1;
        // check whether new entry should be added to end of entry order
        if (-1==insertAfterEntry) {
            // get current last entry
            int cur_last = getLastZettel();
            // if we have no last entry, this one seems to be the last entry
            if (-1==cur_last) {
                // so fix cur_first value
                cur_last = zettelPos;
            }
            // change reference of last entry's next-link
            setNextZettel(cur_last, zettelPos);
            // get current first entry
            int cur_first = getFirstZettel();
            // if we have no first entry, this one seems to be the first entry
            if (-1==cur_first) {
                // so fix cur_first value
                cur_first = zettelPos;
            }
            // change reference of first entry's prev-link
            setPrevZettel(cur_first, zettelPos);
            // sort in entry into entry-order
            setNextZettel(zettelPos, cur_first);
            setPrevZettel(zettelPos, cur_last);
            // set new last entry
            setLastZettel(zettelPos);
        }
        // in this case, we add an entry "in between".
        else {
            // get next-link of insert-entry
            int next_entry = getNextZettel(insertAfterEntry);
            // change next-link to new added entry
            setNextZettel(insertAfterEntry, zettelPos);
            // set pre-link of new added entry to the entry after which
            // this new one has been added
            setPrevZettel(zettelPos, insertAfterEntry);
            // set former insertAfterEntry's next entry's prev-link
            // to this new added entry
            setPrevZettel(next_entry, zettelPos);
            // set next-link of added entry
            setNextZettel(zettelPos, next_entry);
        }
    }
    
    
    /**
     * This method adds a new entry to the datafile. The needed parameters come from the JDialog
     * "CNewEntry.java". This dialog opens an edit-mask so the user can input the necessary information.
     * If everything is done, the JDialog retrieves all the information as string(-array)-variables
     * and simply passes these as paramaters to this method.
     * <br>
     * <br>
     * What we have to do here is to check whether the keywords or links e.g. partly exist, and if so,
     * find out the related index number. Keywords which until now do not already exist in the keyword
     * file have to be added to the keyword file and the new index number has to be addes to the
     * keyword-element of the entry. and so on...
     * 
     * @param title the entry's title as string
     * @param content the entry's content as string
     * @param authors the entry's author as string, retrieve index number and add it to the entry's author-element.
     * use {@code null} if no authors should be added
     * @param keywords the entry's keywords as string-array. retrieve index numbers and add those to the entry's keyword-element
     * use {@code null} if no authors should be added
     * @param remarks the remarks as string
     * @param links the entry's links as string
     * use {@code null} if no authors should be added
     * @param timestamp the current date. in this case, add it as creation date to the timestamp
     * @param luhmann the number of the currently display entry, before the user clicked "new" or "insert entry".
     * if we have to insert an entry, we need to know this number, because that entry retrieves this new entry's
     * index-number and adds it to its luhmann-tag (which indicates follower- and sub-entries).
     * use {@code -1} if no luhmann-number is needed (i.e. no follower-entry is added).
     * @param insertAfterEntry indicates the position, after which existing entry the new added entry should be inserted.
     * Use {@code -1} to add the new entry to the end of the database.
     * @return one of the following constants:<br>
     * {@link #ADD_ENTRY_OK ADD_ENTRY_OK} if a normal entry was successfully added<br>
     * {@link #ADD_LUHMANNENTRY_OK ADD_LUHMANNENTRY_OK} if a follower-entry (trailing entry) was successfully added<br>
     * {@link #ADD_ENTRY_ERR ADD_ENTRY_ERR} if an error occured when adding a normal entry<br>
     * {@link #ADD_LUHMANNENTRY_ERR ADD_LUHMANNENTRY_ERR} if an error occured when adding a follower-entry (trailing entry)
     */
    public int addEntry(String title, String content, String[] authors, String[] keywords, String remarks, String[] links, String timestamp, int luhmann, int insertAfterEntry) {
        return addEntry(title,content,authors,keywords,remarks,links,timestamp,luhmann,false,-1,insertAfterEntry);
    }


    /**
     * This method adds a new entry to the datafile, from an importet bibtex-file. This is the
     * case, if a bibtex-entry has annotations or abstracts, and the user wants automatically
     * to create a new entry from that bibtex-entry.
     * <br><br>
     * In contrary ti the normal {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry} method,
     * we use this one to set an additional xml-attribute to that entry (see {@link #setContentFromBibTexRemark(int) setContentFromBibTexRemark(int)})
     * to indicate that this entry's content was added and automatically created from a bibtex-file. Thus,
     * we can later use this indication to update all entries' contents from a bibtex-file, if the user
     * wants to update them...
     *
     * @param title the entry's title as string
     * @param content the entry's content as string
     * @param authors the entry's author as string, retrieve index number and add it to the entry's author-element.
     * use {@code null} if no authors should be added
     * @param keywords the entry's keywords as string-array. retrieve index numbers and add those to the entry's keyword-element
     * use {@code null} if no authors should be added
     * @param timestamp the current date. in this case, add it as creation date to the timestamp
     * @return one of the following constants:<br>
     * {@link #ADD_ENTRY_OK ADD_ENTRY_OK} if a normal entry was successfully added<br>
     * {@link #ADD_LUHMANNENTRY_OK ADD_LUHMANNENTRY_OK} if a follower-entry (trailing entry) was successfully added<br>
     * {@link #ADD_ENTRY_ERR ADD_ENTRY_ERR} if an error occured when adding a normal entry<br>
     * {@link #ADD_LUHMANNENTRY_ERR ADD_LUHMANNENTRY_ERR} if an error occured when adding a follower-entry (trailing entry)
     */
    public int addEntryFromBibTex(String title, String content, String[] authors, String[] keywords, String timestamp) {
        // add entry
        int succeeded = addEntry(title,content,authors,keywords,"",null,timestamp,-1,false,-1,-1);
        // if operation was successful...
        if (succeeded==ADD_ENTRY_OK || succeeded == ADD_LUHMANNENTRY_OK) {
            // ... set a remark to that entry that it was added from a bibtex-file
            // we might need this in case we want to update this entry from a revised bibtex-file later
            setContentFromBibTexRemark(zettelPos,true);
        }
        return succeeded;
    }


    /**
     * This method sets/adds an indicator to an entry so we know that his entry was automatically
     * created from a bibtex-file (annotation/abstract of a bibtex-entry). We might use this
     * to check which entries have been automatically created and can be updated, when the user
     * re-imports a bibtex-file in purpose to update the Zettelkasten-data.
     *
     * @param pos the entry's index-number. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)}
     * @param val {@code true} if entry's content is from a bibtex-file, {@code false} otherwise
     * (typically not used, since you can use the
     * {@link #addEntry(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String[], java.lang.String, int) addEntry()}
     * method if you want to "normally" add an entry).
     */
    public void setContentFromBibTexRemark(int pos, boolean val) {
        // retrieve requested entry
        Element zettel = retrieveElement(zknFile,pos);
        // if entry does not exist, leave
        if (null==zettel) return;
        // add attribute to indicate that this entry was importet
        // from a bibtex file
        zettel.setAttribute("fromBibTex",(val)?"1":"0");
    }


    /**
     * This method checks whether an entry with the given index number {@code pos}
     * was automatically created from a bibtex file. if so, the XML element
     * contains an attribute {@code fromBibTex} with a value "1".
     *
     * @param pos the entry's index-number. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)}
     * @return {@code true} when the entry was automatically created from a bibtex file,
     * {@code false} otherwise.
     */
    public boolean isContentFromBibTex(int pos) {
        // retrieve requested entry
        Element zettel = retrieveElement(zknFile,pos);
        // if entry does not exist, leave
        if (null==zettel) return false;
        // retrieve indicator, which is stored in an attribute
        String isFromBibTex = zettel.getAttributeValue("fromBibTex");
        // if attribute exists and its value equals 1, we know that this
        // entry was created from a bibtex file
        return (isFromBibTex!=null && isFromBibTex.equals("1"));
    }

    
    /**
     * This method changed an existing entry in the datafile. The needed parameters come from the JDialog
     * "CNewEntry.java". This dialog opens an edit-mask so the user can input the necessary information.
     * If everything is done, the JDialog retrieves all the information as string(-array)-variables
     * and simply passes these as paramaters to this method.
     * <br>
     * <br>
     * What we have to do here is to check whether the keywords or links e.g. partly exist, and if so,
     * find out the related index number. Keywords which until now do not already exist in the keyword
     * file have to be added to the keyword file and the new index number has to be addes to the
     * keyword-element of the entry. and so on...
     * 
     * @param title the entry's title as string
     * @param content the entry's content as string
     * @param authors the entry's authors as string-array, retrieve index number and add it to the entry's author-element
     * @param keywords the entry's keywords as string-array. retrieve index numbers and add those to the entry's keyword-element
     * @param remarks the remarks as string
     * @param links the entry's links as string
     * @param timestamp the current date. in this case, add it as edit date to the timestamp
     * @param entrynumber the number of the entry that should be changed.
     * @return 
     */
    public boolean changeEntry(String title, String content, String[] authors, String[] keywords, String remarks, String[] links, String timestamp, int entrynumber) {
        // create a new zettel-element
        Element zettel = retrieveElement(zknFile,entrynumber);
        // create dummy element
        Element child;
        // if no entry exists, quit
        if (null==zettel) return false;
        // first of all, we remove all authors and keywords from the existing entry
        // we do this to update the frequency of the authors and keywords, so when adding
        // authors/keywords to the data-file, which already belonged to the entry, we would
        // increase the frequency although those authors/keywords are not new
        changeFrequencies(entrynumber,-1);
        // then, create form-images
        createFormImagesFromContent(content);
        try {
            //
            // change title
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_TITLE);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_TITLE);
                // and add it
                zettel.addContent(child);
            }
            // set value of the child element
            child.setText(title);
            //
            // change content
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_CONTENT);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_CONTENT);
                // and add it
                zettel.addContent(child);
            }
            // set value of the child element
            child.setText(content);
            //
            // change author
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_AUTHOR);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_AUTHOR);
                // and add it
                zettel.addContent(child);
            }
            // create empty string buffer which stores the index numbers
            // of the converted authors
            StringBuilder newau = new StringBuilder("");
            // check whether we have authors at all
            if ((authors!=null) && (authors.length>0)) {
                // iterate the array and get the index number of each author string
                // if a keauthoryword does not already exist, add it to the authorfile
                for (String aut : authors) {
                    // trim leading and trailing spaces
                    aut = aut.trim();
                    // only proceed for this entry, if it contains a value
                    if (!aut.isEmpty()) {
                        // add it to the data file
                        // and store the position of the new added author in the
                        // variable authorPos
                        int authorPos = addAuthor(aut,1);
                        // append the index number in the string buffer
                        newau.append(String.valueOf(authorPos));
                        // separator for the the index numbers, since more authors
                        // and thus more index numbers might be stored in the author element
                        newau.append(",");
                    }
                }
                // shorten the stringbuffer by one char, since we have a
                // superfluous comma char (see for-loop above)
                if (newau.length()>0) newau.setLength(newau.length()-1);
            }
            // store author index numbers
            child.setText(newau.toString());
            //
            // change keywords
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_KEYWORD);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_KEYWORD);
                // and add it
                zettel.addContent(child);
            }
            // create empty string buffer which stores the index numbers
            // of the converted keywords
            StringBuilder newkw = new StringBuilder("");
            // check whether we have keywords at all
            if ((keywords!=null) && (keywords.length>0)) {
                // iterate the array and get the index number of each keyword string
                // if a keyword does not already exist, add it to the keywordfile
                for (String keyw : keywords) {
                    // trim leading and trailing spaces
                    keyw = keyw.trim();
                    // only proceed for this entry, if it contains a value
                    if (!keyw.isEmpty()) {
                        // add it to the data file
                        // and store the position of the new added keyword in the
                        // variable keywordPos
                        int keywordPos = addKeyword(keyw,1);
                        // append the index number in the string buffer
                        newkw.append(String.valueOf(keywordPos));
                        // separator for the the index numbers, since more keywords
                        // and thus more index numbers might be stored in the keyword element
                        newkw.append(",");
                    }
                }
                // shorten the stringbuffer by one char, since we have a
                // superfluous comma char (see for-loop above)
                if (newkw.length()>0) newkw.setLength(newkw.length()-1);
            }
            // store keyword index numbers
            child.setText(newkw.toString());
            //
            // change manual links
            //
            addManualLink(entrynumber, extractManualLinksFromContent(content));
            //
            // change hyperlinks
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_ATTACHMENTS);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_ATTACHMENTS);
                // and add it
                zettel.addContent(child);
            }
            // first, remove all existing links, since they are completely
            // set again
            child.removeChildren(ELEMENT_ATTCHILD);
            // add each hyperlink string
            // therefor, iterate the array
            for (String l : links) {
                // create a new subchuld-element
                Element sublink = new Element(ELEMENT_ATTCHILD);
                // and add the link-string from the array
                sublink.setText(l);
                child.addContent(sublink);
            }
            //
            // change remarks
            //
            // retrieve the element
            child = zettel.getChild(ELEMENT_REMARKS);
            // if child-element doesn't exist, add it to the zettel
            if (null==child) {
                // create new child element
                child = new Element(ELEMENT_REMARKS);
                // and add it
                zettel.addContent(child);
            }
            // set value of the content element
            child.setText(remarks);
            //
            // change timestamp
            //
            setTimestampEdited(zettel, timestamp);
            //
            // we don't need any changes on the luhmann number or for
            // manual links here...
            //
            // update the current zettel-position
            zettelPos = entrynumber;
            // and add the new position to the history...
            addToHistory();
            // set modified state
            setModified(true);
        }
        catch (IllegalAddException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return false;
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE, ex.getLocalizedMessage());
            return false;
        }
        return true;
    }
    
    
    /**
     * This method adds a new follower- or sub-entry index-number to an entry. Followers', or
     * sub-entries', index-numbers are stored in the luhmann-tag.
     * <br><br>
     * It is similar to a typical tree: we have one "parent"-entry and several child-entries
     * (sub-entries or followers). each of these child-elements can have their own child-elements again
     * (whereby the child-element itself is then again understood as "parent"-entry).
     * <br><br>
     * So, the Luhmann-numbers of an entry only have one subordinated level of sub-entries. the tree-
     * structure comes from those sub-entries, that might have their own sub-entries again.
     * 
     * @param entry the entry where the related insert-entry-index-number should be added to
     * @param addvalue the index-number of the inserted entry
     * @return {@code true} if everything was ok, false if the addvalue already existed or if the entry
     * indicated by "addvalue" itself already contains the entry "entry". in this case, we would
     * have an infinitive loop, with entry A having a sub-entry B, and B having a sub-entry A again
     * and so on...
     */
    public boolean addLuhmannNumber(int entry, int addvalue) {
        // check whether entry and addvalue are identical
        if (entry==addvalue) return false;
        // get the entry where the luhmann-number should be added to
        Element zettel = retrieveElement(zknFile,entry);
        // get the entry where the luhmann-number should be added to
        Element tobeadded = retrieveElement(zknFile,addvalue);
        // if entry does not exist, leave
        if (null==zettel || null==zettel.getChild(ELEMENT_TRAILS)) return false;
        // if entry does not exist, leave
        if (null==tobeadded) return false;
        // get the luhmann-numbers  of that entry
        String lnr = zettel.getChild(ELEMENT_TRAILS).getText();
        // check whether the addvalue already exists in that entry
        if (!lnr.isEmpty()) {
            // copy all values to an array
            String[] lnrs = lnr.split(",");
            // go throughh array of current luhmann-numbers
            for (String exist : lnrs) {
                try {
                    // if addvalue exist, return false
                    if (Integer.parseInt(exist)==addvalue) return false;
                }
                catch (NumberFormatException ex) {
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                }
            }
        }
        // now we have to check, whether the current entry is already existing
        // in the entry that index-number (addvalue) we want to add to the luhmann-numbers
        // if "entry" already exists in entry "addvalue"'s luhmann-tag, we would have
        // an infinite loop...
        // the problem is, that we here have to recursively check not only the "addvalue"
        // entry's luhmann-tag, but also each sub-entry that consists in the "addvalue"
        // entry's tag...
        lnr = tobeadded.getChild(ELEMENT_TRAILS).getText();
        // check whether the addvalue already exists in that entry
        // if entry exists in the addvalue-entry luhmann-tag, or in any sub-entry
        // of the addvalue-entry, leave method to prevent infinite loops
        if (!lnr.isEmpty() && existsInLuhmann(addvalue, entry, false)) return false;
        // get the luhmann-numbers  of that entry
        StringBuilder sb = new StringBuilder(zettel.getChild(ELEMENT_TRAILS).getText());
        // append separator comma, but only if we already have values
        if (sb.length()>0) sb.append(",");
        // append the addvalue
        sb.append(String.valueOf(addvalue));

/*
        // the the string buffer contains at least two values, we want to sort them
        if (sb.indexOf(",")!=-1) {
            // copy all values of the buffer to an string array
            String[] dummy = sb.toString().split(",");
            // create integer array, because when we sort a string-array,
            // the value "12" would be smaller than "5".
            int[] intdummy = new int[dummy.length];
            // iterate array
            for (int cnt=0; cnt<intdummy.length; cnt++) {
                try {
                    // convert all strings to integer
                    intdummy[cnt] = Integer.parseInt(dummy[cnt]);
                }
                catch (NumberFormatException ex) {
                    CConstants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                }
            }
            // sort the array
            if (intdummy!=null && intdummy.length>0) Arrays.sort(intdummy);
            // reset the string buffer
            sb.setLength(0);
            // iterate the sorted array
            for (int cnt=0; cnt<intdummy.length; cnt++) {
                // and append all values to the string buffer
                sb.append(String.valueOf(intdummy[cnt]));
                sb.append(",");
            }
            // finallay, remove the last ","
            if (sb.length()>1) sb.setLength(sb.length()-1);
        }
*/

        // and set the new string to the luhmann-tag
        zettel.getChild(ELEMENT_TRAILS).setText(sb.toString());
        // addvalue was successfully added
        setModified(true);
        return true;
    }
    
    
    /**
     * This method adds a manual link to an entry.
     * 
     * @param entry the entry where the referred entry-number should be added to
     * @param addvalue the index-number of the referred entry
     * @return {@code true} if everything was ok, false if the addvalue already existed or other errors occured.
     */
    public boolean addManualLink(int entry, int addvalue) {
        // first, add current entry as manual link to the referred entry - we need this to
        // get double-links, from entry a to entry b and back from b to a.
        addManLink(addvalue, entry);
        // now add the referrer-entry-number to the current entry.
        return addManLink(entry, addvalue);
    }
    /**
     * 
     * @param manlinks
     * @param sourceEntry 
     */
    public void addManualLink(List<Integer> manlinks, int sourceEntry) {
        if (manlinks!=null && !manlinks.isEmpty()) {
            // iterate all manual references
            for (int mlcnt : manlinks) {
                // and add backreference to current new entry
                // to all referenced entries
                addManLink(mlcnt, sourceEntry);
            }
        }
    }
    /**
     * 
     * @param sourceEntry
     * @param manlinks 
     */
    public void addManualLink(int sourceEntry, List<Integer> manlinks) {
        if (manlinks!=null && !manlinks.isEmpty()) {
            // iterate all manual references
            for (int mlcnt : manlinks) {
                // and add backreference to current new entry
                // to all referenced entries
                addManualLink(mlcnt, sourceEntry);
            }
        }
    }

    
    /**
     * This method adds a manual link to an entry. It is called 
     * from {@link #addManualLink(int, int) addManualLink}.
     * 
     * @param entry the entry where the referred entry-number should be added to
     * @param addvalue the index-number of the referred entry
     * @return {@code true} if everything was ok, false if the addvalue already existed or other errors occured.
     */
    private boolean addManLink(int entry, int addvalue) {
        // check whether entry and addvalue are identical
        if (entry==addvalue) return false;
        // get the entry where the luhmann-number should be added to
        Element zettel = retrieveElement(zknFile,entry);
        // if entry does not exist, leave
        if (null==zettel || null==zettel.getChild(ELEMENT_MANLINKS)) return false;
        // get the manual links of that entry
        String lnr = zettel.getChild(ELEMENT_MANLINKS).getText();
        // check whether the addvalue already exists in that entry
        if (!lnr.isEmpty()) {
            // copy all values to an array
            String[] lnrs = lnr.split(",");
            // go throughh array of current luhmann-numbers
            for (String exist : lnrs) {
                try {
                    // if addvalue exist, return false
                    if (Integer.parseInt(exist)==addvalue) return false;
                }
                catch (NumberFormatException ex) {
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                }
            }
        }
        // get the luhmann-numbers  of that entry
        StringBuilder sb = new StringBuilder(zettel.getChild(ELEMENT_MANLINKS).getText());
        // append separator comma, but only if we already have values
        if (sb.length()>0) sb.append(",");
        // append the addvalue
        sb.append(String.valueOf(addvalue));
        // the the string buffer contains at least two values, we want to sort them
        if (sb.indexOf(",")!=-1) {
            // copy all values of the buffer to an string array
            String[] dummy = sb.toString().split(",");
            // create integer array, because when we sort a string-array,
            // the value "12" would be smaller than "5".
            int[] intdummy = new int[dummy.length];
            // iterate array
            for (int cnt=0; cnt<intdummy.length; cnt++) {
                try {
                    // convert all strings to integer
                    intdummy[cnt] = Integer.parseInt(dummy[cnt]);
                }
                catch (NumberFormatException ex) {
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                }
            }
            // sort the array
            if (intdummy.length>0) Arrays.sort(intdummy);
            // reset the string buffer
            sb.setLength(0);
            // iterate the sorted array
            for (int cnt=0; cnt<intdummy.length; cnt++) {
                // and append all values to the string buffer
                sb.append(String.valueOf(intdummy[cnt]));
                sb.append(",");
            }
            // finallay, remove the last ","
            if (sb.length()>1) sb.setLength(sb.length()-1);
        }
        // and set the new string to the manlinks-tag
        zettel.getChild(ELEMENT_MANLINKS).setText(sb.toString());
        // addvalue was successfully added
        setModified(true);
        return true;
    }

    
    /**
     * Removes a certain entry-number from the luhmann-numbers of an entry.
     * 
     * @param entry the entry where a luhmann-number should be removed
     * @param removevalue the index-number that should be removed from "entry"
     */
    public void deleteLuhmannNumber(int entry, int removevalue) {
        // check whether entry and removevalue are identical
        if (entry==removevalue) return;
        // get the entry where the luhmann-number should be added to
        Element zettel = retrieveElement(zknFile,entry);
        // if entry does not exist, leave
        if (null==zettel || null==zettel.getChild(ELEMENT_TRAILS)) return;
        // get the luhmann-numbers  of that entry
        String lnr = zettel.getChild(ELEMENT_TRAILS).getText();
        // check whether the addvalue already exists in that entry
        if (!lnr.isEmpty()) {
            // copy all values to an array
            String[] lnrs = lnr.split(",");
            // create new string buffer for the final values
            StringBuilder sb = new StringBuilder("");
            // convert remove-value to string, so we can compare
            String removenr = String.valueOf(removevalue);
            // go through array of current luhmann-numbers
            for (String exist : lnrs) {
                // if the current luhhmann-number is not the one which should be deleted...
                if (!exist.equals(removenr)) {
                    // ...add it to the buffer
                    sb.append(exist);
                    sb.append(",");
                }
            }
            // finally, remove trailing comma
            if (sb.length()>1) sb.setLength(sb.length()-1);
            // and set the new string to the luhmann-tag
            zettel.getChild(ELEMENT_TRAILS).setText(sb.toString());
            // addvalue was successfully added
            setModified(true);
        }
    }


    /**
     * This method inserts the entry-number {@code insertnr} as luhmann-number at the position
     * {@code pos} within the entry's {@code entry} luhmann-numbers.
     *
     * @param entry the entry where the luhmann-number {@code insertnr} should be inserted
     * @param insertnr the number of the entry that should be added as luhmann-number
     * @param pos the position of the {@code insertnr}, i.e. at which position {@code insertnr}
     * should be added as luhmann-number
     * @return 
     */
    public boolean insertLuhmannNumber(int entry, int insertnr, int pos) {
        // check whether entry and removevalue are identical
        if (entry==insertnr) return false;
        // get the entry where the luhmann-number should be added to
        Element zettel = retrieveElement(zknFile,entry);
        // get the entry that should be added as luhmann-number
        Element tobeadded = retrieveElement(zknFile,insertnr);
        // if entry does not exist, leave
        if (null==zettel || null==zettel.getChild(ELEMENT_TRAILS)) return false;
        // get the luhmann-numbers  of that entry
        String lnr = zettel.getChild(ELEMENT_TRAILS).getText();
        // check whether the addvalue already exists in that entry
        if (!lnr.isEmpty()) {
            // now we have to check, whether the current entry is already existing
            // in the entry that index-number (addvalue) we want to add to the luhmann-numbers
            // if "entry" already exists in entry "addvalue"'s luhmann-tag, we would have
            // an infinite loop...
            // the problem is, that we here have to recursively check not only the "addvalue"
            // entry's luhmann-tag, but also each sub-entry that consists in the "addvalue"
            // entry's tag...
            String alnr = tobeadded.getChild(ELEMENT_TRAILS).getText();
            // check whether the addvalue already exists in that entry
            // if entry exists in the addvalue-entry luhmann-tag, or in any sub-entry
            // of the addvalue-entry, leave method to prevent infinite loops
            if (!alnr.isEmpty() && existsInLuhmann(insertnr, entry, false)) return false;
            // copy all values to an array
            String[] lnrs = lnr.split(",");
            // create list
            List<String> luhmannnrs = new ArrayList<String>();
            // copy all numbers to list, so we can insert the new number via this list
            // for (String ln : lnrs) luhmannnrs.add(ln);
            luhmannnrs.addAll(Arrays.asList(lnrs));
            try {
                // now insert the new number
                luhmannnrs.add(pos, String.valueOf(insertnr));
            }
            catch (IndexOutOfBoundsException e) {
                // if the index-number was out of bounds, append number to the end of the list
                luhmannnrs.add(String.valueOf(insertnr));
            }
            // create stringbuilder
            StringBuilder sb = new StringBuilder("");
            for (String luhmannnr : luhmannnrs) {
                sb.append(luhmannnr).append(",");
            }
            // finally, remove trailing comma
            if (sb.length()>1) sb.setLength(sb.length()-1);
            // and set the new string to the luhmann-tag
            zettel.getChild(ELEMENT_TRAILS).setText(sb.toString());
            // addvalue was successfully added
            setModified(true);
            // return success
            return true;
        }
        // return success
        return false;
    }

    
    /**
     * Removes one or more manual links from the current entry...
     * 
     * @param manlinks an integer-array with the manual links that should be removed...
     */
    public void deleteManualLinks(String[] manlinks) {
        // get the current manual links...
        int[] current_mls = getCurrentManualLinks();
        // if no manual links available, leave...
        if ((null==current_mls)||(current_mls.length<1)) return;
        // if no manual links from parameter available, leave...
        if ((null==manlinks)||(manlinks.length<1)) return;
        // create linked list and copy all current manual links to that list
        LinkedList<String> l = new LinkedList<String>();
        for (int ml : current_mls) l.add(String.valueOf(ml));
        // go through all entries that should be removed from the manual links...
        for (String mlparam : manlinks) {
            // find the entry in the linked list
            int pos = l.indexOf(mlparam);
            // if it exists, remove it.
            if (pos!=-1) {
                l.remove(pos);
                // we also have to remove the *current* entry from the referred entry
                try {
                    int mlparamentry = Integer.parseInt(mlparam);
                    // therefore, get the manual links from the referred entry "mlparam"
                    String[] backlinks = getManualLinksAsString(mlparamentry);
                    // create new stringbuilder
                    StringBuilder sb = new StringBuilder("");
                    // get current entry position as string. we need to remove this value
                    // from the referred entry's manual links, given in the array "backlinks"
                    String curentry = String.valueOf(zettelPos);
                    // go through all manual links of the referred entry
                    for (String bl : backlinks) {
                        // if the manual link of the referred entry is *not* the current entry...
                        if (!bl.equals(curentry)) {
                            // append it to the string builder
                            sb.append(bl);
                            sb.append(",");
                        }
                    }
                    // delete last comma
                    if (sb.length()>1) sb.setLength(sb.length()-1);
                    // and update manual links of the referred entry
                    setManualLinks(mlparamentry,sb.toString());
                }
                catch (NumberFormatException e) {
                    Constants.zknlogger.log(Level.WARNING,e.getLocalizedMessage());
                }
            }
        }
        // now we have all remaining manual links in the linked list "l". we now copy
        // each element of that list to a string-builder and set that string as new ELEMENT_MANLINKS
        // value for the current entry...
        StringBuilder sb = new StringBuilder("");
        // create an iterator
        Iterator<String> i = l.iterator();
        // go through list
        while (i.hasNext()) {
            // add each element to stringbuilder
            sb.append(i.next());
            sb.append(",");
        }
        // truncate last comma
        if (sb.length()>1) sb.setLength((sb.length()-1));
        // update manual links of current entry
        setManualLinks(zettelPos,sb.toString());
    }
    
    
    /**
     * This method returns the content of an entry's luhmann-tag, i.e. the follower-
     * or sub-entries of an entry. These numbers are displayed in the tabbedpane in the
     * jTreeLuhmann (see ZettelkastenView.java for more details).
     * 
     * @param pos the position of the entry which luhmann-numbers we want to have
     * @return a string with the comma-separated luhmann-numbers, or an empty string if the entry
     * has not luhmann-numbers.
     */
    public String getLuhmannNumbers(int pos) {
        // get the entry
        Element zettel = retrieveElement(zknFile,pos);
        // if it exists...
        // return the content of the luhmann-child-element
        if (zettel!=null&&zettel.getChild(ELEMENT_TRAILS)!=null) return zettel.getChild(ELEMENT_TRAILS).getText();
        // return result
        return "";
    }
    /**
     * This method returns the luhmann-numbers (follower-links) for an entry as string-array.
     *
     * @param pos the position of the entry which luhmann-numbers we want to have
     * @return an string-array containing the follower-entry-numbers where the entry {@code pos} refers to,
     * or {@code null} if no such follower-entry-numbers exist...
     */
    public String[] getLuhmannNumbersAsString(int pos) {
        // get manual links
        String luh = getLuhmannNumbers(pos);
        // if no manual links there, quit...
        if (luh.isEmpty()) return null;
        // else split them into an array...
        String[] luhmann = luh.split(",");
        // if we have no manual links, return null...
        if ((null==luhmann)||luhmann.length<1) return null;
        // return the content of the luhmann-child-element
        return luhmann;
    }
    /**
     * This method returns the luhmann-numbers (follower-links) for an entry as integer-array.
     *
     * @param pos the position of the entry which luhmann-numbers we want to have
     * @return an integer-array containing the follower-entry-numbers where the entry {@code pos} refers to,
     * or {@code null} if no such follower-entry-numbers exist...
     */
    public int[] getLuhmannNumbersAsInteger(int pos) {
        // get manual links
        String luh = getLuhmannNumbers(pos);
        // if no manual links there, quit...
        if (luh.isEmpty()) return null;
        // else split them into an array...
        String[] luhmann = luh.split(",");
        // if we have no manual links, return null...
        if ((null==luhmann)||luhmann.length<1) return null;
        // create integer array
        int[] luhint = new int[luhmann.length];
        // copy string to int
        for (int cnt=0; cnt<luhmann.length; cnt++) {
            try {
                luhint[cnt] = Integer.parseInt(luhmann[cnt]);
            }
            catch (NumberFormatException ex) {
            }
        }
        return luhint;
    }
    
    
    /**
     * This method returns the manual links for an entry as integer-array.
     * 
     * @param pos the position of the entry which manual links we want to have
     * @return an integer array containing the entry-numbers where the current entry refers to,
     * or null if no entry-numbers exist...
     */
    public int[] getManualLinks(int pos) {
        // get Manual Links as String Array
        String[] manlinks = getManualLinksAsString(pos);
        // if we have no manual links, return null...
        if ((null==manlinks)||manlinks.length<1) return null;
        // create return value
        int[] retval = new int[manlinks.length];
        // copy all string-numbers to int-array
        for (int cnt=0; cnt<manlinks.length; cnt++) retval[cnt] = Integer.parseInt(manlinks[cnt]);
        // return the content of the luhmann-child-element
        return retval;
    }
    /**
     * Sets the manual links for an entry. these links appear in the main-window's tabbed pane
     * on the "links"-page, in the jTableManLinks.
     * 
     * @param pos the entry-number that should get new manlinks
     * @param manlinks the entry-numbers where the entry "pos" refers to, stored in an integer-array
     */
    public void setManualLinks(int pos, int[] manlinks) {
        // get the entry
        Element zettel = retrieveElement(zknFile,pos);
        // if we found an entry-element, go on
        if (zettel!=null) {
            // if no child-element ELEMENT_MANLINKS exists, create it...
            if (null==zettel.getChild(ELEMENT_MANLINKS)) zettel.addContent(new Element(ELEMENT_MANLINKS));
            // create stringbuilder
            StringBuilder sb = new StringBuilder("");
            // iterate int-array
            for (int ml : manlinks) {
                // and copy all int-values to array
                sb.append(String.valueOf(ml));
                sb.append(",");
            }
            // delete last comma
            if (sb.length()>1) sb.setLength(sb.length()-1);
            // and set value to element...
            zettel.getChild(ELEMENT_MANLINKS).setText(sb.toString());
            setModified(true);
        }
    }

    
    /**
     * This method returns the manual links for an entry as string-array.
     * 
     * @param pos the position of the entry which manual links we want to have
     * @return an string-array containing the entry-numbers where the entry {@code pos} refers to,
     * or {@code null} if no entry-numbers exist...
     */
    public String[] getManualLinksAsString(int pos) {
        // get the entry
        Element zettel = retrieveElement(zknFile,pos);
        // if it exists...
        if (zettel!=null&&zettel.getChild(ELEMENT_MANLINKS)!=null) {
            // get manual links
            String ml = zettel.getChild(ELEMENT_MANLINKS).getText();
            // if no manual links there, quit...
            if (ml.isEmpty()) return null;
            // else split them into an array...
            String[] manlinks = ml.split(",");
            // if we have no manual links, return null...
            if ((null==manlinks)||manlinks.length<1) return null;
            // return the content of the luhmann-child-element
            return manlinks;
        }
        // return result
        return null;
    }
    /**
     * This method returns the manual links for an entry as siingle string with
     * comma separated values
     * 
     * @param pos the position of the entry which manual links we want to have
     * @return siingle string with comma separated values where the entry {@code pos} refers to,
     * or {@code null} if no entry-numbers exist...
     */
    public String getManualLinksAsSingleString(int pos) {
        // get the entry
        Element zettel = retrieveElement(zknFile,pos);
        // if it exists...
        if (zettel!=null&&zettel.getChild(ELEMENT_MANLINKS)!=null) {
            // get manual links
            String ml = zettel.getChild(ELEMENT_MANLINKS).getText();
            // if no manual links there, quit...
            if (ml.isEmpty()) return null;
            // else split them into an array...
            // return the content of the luhmann-child-element
            return ml;
        }
        // return result
        return null;
    }
    /**
     * Sets the manual links for an entry. these links appear in the main-window's tabbed pane
     * on the "links"-page, in the jTableManLinks.
     * 
     * @param pos the entry-number that should get new manlinks
     * @param manlinks the entry-numbers where the entry "pos" refers to, stored in a string-value
     * (comma-separated)
     */
    public void setManualLinks(int pos, String manlinks) {
        // get the entry
        Element zettel = retrieveElement(zknFile,pos);
        // if we found an entry-element, go on
        if (zettel!=null) {
            // if no child-element ELEMENT_MANLINKS exists, create it...
            if (null==zettel.getChild(ELEMENT_MANLINKS)) zettel.addContent(new Element(ELEMENT_MANLINKS));
            // and set value to element...
            zettel.getChild(ELEMENT_MANLINKS).setText(manlinks);
            setModified(true);
        }
    }

    
    /**
     * This method returns the manual links for the current entry as integer-array.
     * 
     * @return an integer array containing the entry-numbers where the current entry refers to,
     * or null if no entry-numbers exist...
     */
    public int[] getCurrentManualLinks() {
        return getManualLinks(zettelPos);
    }
    
    
    /**
     * This method returns the manual links for the current entry as string-array.
     * 
     * @return a string array containing the entry-numbers where the current entry refers to,
     * or null if no entry-numbers exist...
     */
    public String[] getCurrentManualLinksAsString() {
        return getManualLinksAsString(zettelPos);
    }
    
    
    /**
     * This method checks, whether a given value already exist in an entry's luhmann-tag, or
     * in any of the entry's sub-entries luhmann-tags. We need this to prevent infinite loops
     * when displaying the sub-entries. An entry A may contain an entry B as sub-entry, but
     * entry B or any of entry B's sub-entries may not contain entry A!
     * 
     * @param entry (the entry which luhmann-tag we want to check)
     * @param checkvalue (the entry which may not part of entry's luhmann-tag)
     * @param found (whether the checkvalue already exists in the enry's luhmann-tag or not, initially should be "false")
     * @return {@code true} when the checkvalue exists, false otherwise. actually the "found"-value is returned
     */
    private boolean existsInLuhmann(int entry, int checkvalue, boolean found) {
        // if we found anything by now, return true
        if (found) return true;
        // get the entry
        Element zettel = retrieveElement(zknFile,entry);
        // if it exists, go on
        if (zettel!=null&&zettel.getChild(ELEMENT_TRAILS)!=null) {
            // get the text from the luhmann-numbers
            String lnr = zettel.getChild(ELEMENT_TRAILS).getText();
            // if we have any luhmann-numbers, go on...
            if (!lnr.isEmpty()) {
                // copy all values to an array
                String[] lnrs = lnr.split(",");
                // go throughh array of current luhmann-numbers
                for (String exist : lnrs) {
                    // check whether luhmann-value exists, by re-calling this method
                    // again and go through a recusrive loop
                    found = existsInLuhmann(Integer.parseInt(exist), checkvalue, found);
                    // if we have found a check-value, return true
                    if (found) return true;
                    // else check whether the current entry equals the checkvalue
                    found = (entry==checkvalue);
                }
            }
            // else check whether the current entry equals the checkvalue
            else {
                found = (entry==checkvalue);
            }
        }
        // return result
        return found;
    }
    
    
    /**
     * This methods returns the author of a given position in the <b>author-datafile</b>.<br><br>
     * This method is used for creating the literatur list which is displayed in a 
     * table on the JTabbedPane of the main window.<br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param pos a valid position of an element, ranged from 1 to {@link #getCount(int) getCount(AUCOUNT)}
     * @return the author string or an empty string if nothing was found
     */
    public String getAuthor(int pos) {
        // retrieve the author element
        Element author = retrieveElement(authorFile, pos);
        // return the matching string value of the author element
        String retval;
        // check whether the element is null
        if (null==author) retval = "";
        else retval = author.getText();
        
        return retval;
    }

    
    /**
     * This method sets an author to a given position in the author datafile
     * could be used for overwriting/changing existing authors
     * 
     * @param pos the position of the author. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)}
     * @param auth the author string itself
     */
    public void setAuthor(int pos, String auth) {
        setAuthorValue(pos,auth,null,-1);
    }
    /**
     * This method sets an author to a given position in the author datafile
     * could be used for overwriting/changing existing authors, including a
     * new bibkey value
     * 
     * @param pos the position of the author. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)}
     * @param auth the author string itself
     * @param bibkey optional, a bibkey reference for the author
     */
    public void setAuthor(int pos, String auth, String bibkey) {
        setAuthorValue(pos,auth,bibkey,-1);
    }
    /**
     * This method sets an author to a given position in the author datafile
     * could be used for overwriting/changing existing authors
     *
     * @param pos the position of the author. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)}
     * @param auth the author string itself
     * @param freq the frequency of the author. use <b>-1</b> when the frequency-attribute
     * should be left unchanged.
     */
    public void setAuthor(int pos, String auth, int freq) {
        setAuthorValue(pos,auth,null,freq);
    }
    /**
     * This method sets an author to a given position in the author datafile
     * could be used for overwriting/changing existing authors, including a
     * new bibkey value
     *
     * @param pos the position of the author. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)}
     * @param auth the author string itself
     * @param bibkey optional, a bibkey reference for the author
     * @param freq the frequency of the author. use <b>-1</b> when the frequency-attribute
     * should be left unchanged.
     */
    public void setAuthor(int pos, String auth, String bibkey, int freq) {
        setAuthorValue(pos,auth,bibkey,freq);
    }
    /**
     * This method sets an author to a given position in the author datafile
     * could be used for overwriting/changing existing authors
     *
     * @param pos the position of the author. The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)}
     * @param auth the author string itself
     * @param freq the frequency of the author. use <b>-1</b> when the frequency-attribute
     * should be left unchanged.
     */
    private void setAuthorValue(int pos, String auth, String bibkey, int freq) {
        // retrieve author
        Element author = retrieveElement(authorFile,pos);
        // if a valid element was found...
        if (author!=null) {
            // ...set the new text
            author.setText(auth);
            // and new frequency, but only if it is not -1
            if (freq!=-1) author.setAttribute(ATTRIBUTE_FREQUENCIES, String.valueOf(freq));
            // change bibkey
            if (bibkey!=null) {
                setAuthorBibKey(auth, bibkey.trim());
            }
            // and change the modified state of the file
            setModified(true);
        }
    }


    /**
     * This method deletes an author by removing the content from the element
     * inside of the author xml datafile. the element itself is kept and left
     * empty. this ensures that the order and numbering of an author never
     * changes. Since the zettelkasten datafile stores the index-numbers of the authors
     * a changing in the position/order/numbering of the author datafile would lead
     * to corrupted author associations in the zettelkasten data file
     * 
     * @param pos position of author which should be deleted
     */
    public void deleteAuthor(int pos) {
        // check whether author exists...
        if (!getAuthor(pos).isEmpty()) {
            // ...delete its content
            // therefore, get the author's index-number as string (for comparison below)
            String nr = String.valueOf(pos);
            // create new string buffer
            StringBuilder newau = new StringBuilder("");
           // and delete this index-number from all entries
            for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
                // get each element
                Element zettel = retrieveElement(zknFile,cnt);
                // get the author-index-numbers
                String[] aunr = zettel.getChild(ELEMENT_AUTHOR).getText().split(",");
                // reset buffer
                newau.setLength(0);
                for (String aunr1 : aunr) {
                    // if deleted value does not equal author-value, add it
                    if (!aunr1.equals(nr)) {
                        // append index-number
                        newau.append(aunr1);
                        // and a seperator-comma
                        newau.append(",");
                    }
                }
                // shorten the stringbuffer by one char, since we have a
                // superfluous comma char (see for-loop above)
                if (newau.length()>0) newau.setLength(newau.length()-1);
                // now set the new author-index-numbers to the zettel
                zettel.getChild(ELEMENT_AUTHOR).setText(newau.toString());
            }
            // we don't want to remove the element itself, because this would lead
            // to changing index-numbers/element-position within the document. however,
            // an author should ever keep the same index-number. rather, we could fill
            // this "empty space" with new authors
            Element author = retrieveElement(authorFile,pos);
            // if we have an author, go on...
            if (author!=null) {
                // clear text
                author.setText("");
                // and reset attributes
                author.setAttribute(ATTRIBUTE_FREQUENCIES,"0");
                author.setAttribute(ATTRIBUTE_AUTHOR_ID, "");
                author.setAttribute(ATTRIBUTE_AUTHOR_TIMESTAMP, "");
                // and reset bibkey
                author.setAttribute(ATTRIBUTE_AUTHOR_BIBKEY,"");
            }
            // and change modified state
            setModified(true);
        }
    }
    
    
    /**
     * This function retrieves the title, content and author of an entry and
     * "converts" the data into a certain html layout which then appears in the
     * main window's textfield (jEditorPane).
     * <br><br>
     * <b>Caution!</b> Remember that {@code pos} has a range <i>from 1 to (size of zknfile)</i>, so we can directly
     * use the index number which are displayed in the jTable of the main window. However,
     * the access to the xml files are ranged between 0 and size-1, but this is achieved
     * in the retrieveElement-method, where we use "pos-1" to locate the correct entry
     * <br><br>
     * Use {@link #getZettelContent(int) getZettelContent(int)} if you need the plain entry
     * content as it is stored in the XML-file, without htnml-conversion.
     * @param pos the entry-number. use a number from 1 to {@link #getCount(int) getCount(ZKNCOUNT)}
     * @param segmentKeywords the keywords that are associated with certain segments or paragraphs of
     * that entry, so these paragraphs associated with an entry will be highlighted, when the entry
     * is selected in the main window. use {@code null} if not needed.
     * @param sourceframe a reference to the frame from where this function call came. needed for
     * the html-formatting, since entries are differently formatted in the search window.
     * @return a string array with the html layoutet content of the requested entry and author
     */
    public String getEntryAsHtml(int pos, String[] segmentKeywords, int sourceframe) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry) return "";
        // pass the title, content and author information to the html class
        // this class is responsible for doing the layout of the html page
        // which display an entry in the main window's JEditorPane
        // return the complete html page as string array, first element of the
        // array containing the main entry, second element the author information
        return HtmlUbbUtil.getEntryAsHTML(settings, this, bibtexObj, pos, segmentKeywords, sourceframe);
    }

    
    /**
     * This method retrieves the rating-attribute of entries and return the current rating
     * of entry {@code nr} as float-value.<br><br>
     * Rating can be a value from 0 to 5, including decimal place.
     *
     * @param nr the number of the entry which rating-value is requested. <b>Caution!</b> The parameter
     * {@code nr} has to be value from <i>1 to (size of {@link #getCount(int) getCount(ZKNCOUNT)})</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * @return the rating of the entry as float-value, or {@code 0} (zero) if no rating exists.
     */
    public float getZettelRating(int nr) {
        // check for valid parameter. If number-parameter is out of
        // bound, return 0
        if (nr<1 || nr>getCount(Daten.ZKNCOUNT)) return 0;
        // get entry
        Element entry = retrieveZettel(nr);
        // check for value return value
        if (entry!=null) {
            // retrieve rating-attribute
            String rating = entry.getAttributeValue(ATTRIBUTE_RATING);
            // check whether rating-attribute exists
            if (rating!=null && !rating.isEmpty()) {
                try {
                    // try to convert value into float-variable and return that result
                    float rateval = Float.parseFloat(rating);
                    // and round to retrieve only one decimal place
                    return (float) ((float) Math.round(rateval*10) / 10.0);
                }
                catch (NumberFormatException ex) {
                    // log error
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                    Constants.zknlogger.log(Level.WARNING, "Could not parse rating value of entry {0}. Atribute-value is \"{1}\".", new Object[]{String.valueOf(nr), rating});
                }
            }
        }
        // nothing found, return 0
        return 0;
    }


    /**
     * 
     * @param nr
     * @return
     */
    public int getZettelRatingCount(int nr) {
        // check for valid parameter. If number-parameter is out of
        // bound, return 0
        if (nr<1 || nr>getCount(Daten.ZKNCOUNT)) return 0;
        // get entry
        Element entry = retrieveZettel(nr);
        // check for value return value
        if (entry!=null) {
            // retrieve rating-attribute
            String ratingcount = entry.getAttributeValue(ATTRIBUTE_RATINGCOUNT);
            // check whether rating-attribute exists
            if (ratingcount!=null && !ratingcount.isEmpty()) {
                try {
                    // try to convert value into float-variable and return that result
                    return Integer.parseInt(ratingcount);
                }
                catch (NumberFormatException ex) {
                    // log error
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                    Constants.zknlogger.log(Level.WARNING, "Could not parse rating-count value of entry {0}. Atribute-value is \"{1}\".", new Object[]{String.valueOf(nr), ratingcount});
                }
            }
        }
        // nothing found, return 0
        return 0;
    }


    /**
     * This method retrieves the rating-attribute of entries and return the current rating
     * of entry {@code nr} as string-value.<br><br>
     * Rating can be a value from 0 to 5, including decimal place.
     *
     * @param nr the number of the entry which rating-value is requested. <b>Caution!</b> The parameter
     * {@code nr} has to be value from <i>1 to (size of {@link #getCount(int) getCount(ZKNCOUNT)})</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * @return the rating of the entry as string-value, or {@code "0"} if no rating exists.
     */
    public String getZettelRatingAsString(int nr) {
        // get entry-rating as float-value and return it as string
        return String.valueOf(getZettelRating(nr));
    }


    /**
     *
     * @param nr
     * @param rate
     * @return
     */
    public boolean addZettelRating(int nr, float rate) {
        // check for valid parameter. If number-parameter is out of
        // bound, return 0
        if (nr<1 || nr>getCount(Daten.ZKNCOUNT)) return false;
        // get entry
        Element entry = retrieveZettel(nr);
        // check for value return value
        if (entry!=null) {
            // check whether rating-attribute exists
            String rating = entry.getAttributeValue(ATTRIBUTE_RATING);
            // if attribute does not exist, create it
            if (null==rating) {
                // set rating-attribute
                entry.setAttribute(ATTRIBUTE_RATING, String.valueOf(rate));
                // set rating count value to 1
                entry.setAttribute(ATTRIBUTE_RATINGCOUNT, "1");
                // set modified state
                setModified(true);
                // title list has to be updated
                setTitlelistUpToDate(false);
                // and quit
                return true;
            }
            else {
                // check whether rating-count value exists
                String ratingcount = entry.getAttributeValue(ATTRIBUTE_RATINGCOUNT);
                // if attribute does not exist, we have no base to calculate the average
                // rating, so we "reset" the rating by setting the new rating as default
                if (null==ratingcount) {
                    // log error
                    Constants.zknlogger.log(Level.WARNING, "Could not find rating-count attribute of entry {0}. The rating was reset to default value.", String.valueOf(nr));
                    // set rating-attribute
                    entry.setAttribute(ATTRIBUTE_RATING, String.valueOf(rate));
                    // set rating count value to 1
                    entry.setAttribute(ATTRIBUTE_RATINGCOUNT, "1");
                    // set modified state
                    setModified(true);
                    // title list has to be updated
                    setTitlelistUpToDate(false);
                    // and quit
                    return true;
                }
                // now calculate new average rating
                try {
                    // try to convert value into float-variable and return that result
                    float ratingvalue = Float.parseFloat(rating);
                    // convert rating count
                    int ratingcnt = Integer.parseInt(ratingcount);
                    // check for valid values, i.e. if we have already rating-values,
                    // and not for instance reset values.
                    if (ratingcnt>0 && ratingvalue>0.0) {
                        // calulate new rating
                        float newrating = (float) (((ratingvalue*ratingcnt)+rate)/(ratingcnt+1));
                        // set back new values
                        entry.setAttribute(ATTRIBUTE_RATING, String.valueOf(newrating));
                        entry.setAttribute(ATTRIBUTE_RATINGCOUNT, String.valueOf(ratingcnt+1));
                    }
                    // in case we have reset-values, set new rate as default value
                    else {
                        // set rating-attribute
                        entry.setAttribute(ATTRIBUTE_RATING, String.valueOf(rate));
                        // set rating count value to 1
                        entry.setAttribute(ATTRIBUTE_RATINGCOUNT, "1");
                    }
                    // set modified state
                    setModified(true);
                    // title list has to be updated
                    setTitlelistUpToDate(false);
                    // return success
                    return true;
                }
                catch (NumberFormatException ex) {
                    // log error
                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                    Constants.zknlogger.log(Level.WARNING, "Could not parse rating value of entry {0}. Atribute-value is \"{1}\".", new Object[]{String.valueOf(nr), rating});
                    Constants.zknlogger.log(Level.WARNING, "Could not parse rating-count value of entry {0}. Atribute-value is \"{1}\".", new Object[]{String.valueOf(nr), ratingcount});
                }

            }
        }
        return false;
    }


    /**
     *
     * @param nr
     */
    public void resetZettelRating(int nr) {
        // check for valid parameter. If number-parameter is out of
        // bound, return 0
        if (nr<1 || nr>getCount(Daten.ZKNCOUNT)) return;
        // get entry
        Element entry = retrieveZettel(nr);
        // check for value return value
        if (entry!=null) {
            // reset rating-values
            entry.setAttribute(ATTRIBUTE_RATING, "0");
            entry.setAttribute(ATTRIBUTE_RATINGCOUNT, "0");
            // set modified state
            setModified(true);
            // title list has to be updated
            setTitlelistUpToDate(false);
        }
    }


    /**
     * Checks whether an entry at the given {@code pos} is empty (thus deleted) or not.
     * @param pos the entry-number of that entry which has to be checked
     * @return {@code true} when the entry with the number {@code pos} is empty, false otherwise
     */
    public boolean isEmpty(int pos) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return false
        if (null==entry) return true;
        // else return whether content available or not
        return entry.getChild(ELEMENT_CONTENT).getText().isEmpty();
    }
    /**
     * Checks whether an entry at the given {@code pos} is empty (thus deleted) or not.
     * @param entry
     * @return {@code true} when the entry with the number {@code pos} is empty, false otherwise
     */
    public boolean isEmpty(Element entry) {
        // if no element exists, return false
        if (null==entry) return true;
        // else return whether content available or not
        return entry.getChild(ELEMENT_CONTENT).getText().isEmpty();
    }
    /**
     * Checks whether an entry at the given {@code pos} is deleted or not.
     * @param pos the entry-number of that entry which has to be checked
     * @return {@code true} when the entry with the number {@code pos} is deleted, false otherwise
     */
    public boolean isDeleted(int pos) {
        return isEmpty(pos);
    }
    /**
     * Checks whether an entry at the given {@code pos} is deleted or not.
     * @param entry the entry-element of that entry which has to be checked
     * @return {@code true} when the entry with the number {@code pos} is deleted, false otherwise
     */
    public boolean isDeleted(Element entry) {
        return isEmpty(entry);
    }

    
    /**
     * This method checks an XML-database for existing entries.
     * @return {@code true} if the XML-file contains valid entries,
     * {@code false} if the XML-file contains no or only deleted entries.
     */
    public boolean hasEntriesExcludingDeleted() {
        if (getCount(ZKNCOUNT)<1) return false;
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            if (!isDeleted(cnt)) return true;
        }
        return false;
    }

    /**
     * This method returns the timestamp of the entry "pos". the timestamp is returned in a string-array.
     * the first part contains the created date, the second patr the edited date
     * @param pos the entry-number which timestamp we want to have
     * @return a stringarray, the first part holding the created date, the second part the edited date - or
     * {@code null} if no timestamp available.
     */
    public String[] getTimestamp(int pos) {
        return getTimestamp(retrieveZettel(pos));
    }
    /**
     * This method returns the timestamp of the entry "entry". the timestamp is returned in a string-array.
     * the first part contains the created date, the second patr the edited date
     * @param entry the entry-element which timestamp we want to have
     * @return a stringarray, the first part holding the created date, the second part the edited date - or
     * {@code null} if no timestamp available.
     */
    public String[] getTimestamp(Element entry) {
        // if no element exists, return empty array
        if (null==entry) return null;
        // get created date
        String created = entry.getAttributeValue(ATTRIBUTE_TIMESTAMP_CREATED);
        // get edited date
        String edited = entry.getAttributeValue(ATTRIBUTE_TIMESTAMP_EDITED);
        return new String[] {created,edited};
    }
    /**
     * This method returns the last modification (edited) timestamp of the entry "entry".
     * 
     * @param entry the entry-element which edit-timestamp (last modification) we want to have
     * @return a string containig the edited date - or {@code null} if no timestamp available.
     */
    public String getTimestampEdited(Element entry) {
        // if no element exists, return empty array
        if (null==entry) return null;
        // return edited date
        return entry.getAttributeValue(ATTRIBUTE_TIMESTAMP_EDITED);
    }
    /**
     * This method returns the last modification (edited) timestamp of the entry "entry".
     * 
     * @param nr the entry-number which edit-timestamp (last modification) we want to have
     * @return a string containig the edited date - or {@code null} if no timestamp available.
     */
    public String getTimestampEdited(int nr) {
        return getTimestampEdited(retrieveZettel(nr));
    }
    /**
     * This method returns the creation timestamp of the entry "entry".
     * 
     * @param entry the entry-element which created-timestamp we want to have
     * @return a string containig the creation date of that entry - or {@code null} if no timestamp available.
     */
    public String getTimestampCreated(Element entry) {
        // if no element exists, return empty array
        if (null==entry) return null;
        // return edited date
        return entry.getAttributeValue(ATTRIBUTE_TIMESTAMP_CREATED);
    }
    /**
     * This method returns the last modification (edited) timestamp of the entry "entry".
     * 
     * @param nr the entry-number which created-timestamp we want to have
     * @return a string containig the creation date of that entry - or {@code null} if no timestamp available.
     */
    public String getTimestampCreated(int nr) {
        return getTimestampCreated(retrieveZettel(nr));
    }


    /**
     * This method returns the links of an entry. since we can have more than just one 
     * link/hyperlink per entry, the return-value is of the type {@code List<Element>}, i.e.
     * we return a list of xml-elements which contain the links of an entry.
     * 
     * @param pos the entry from which we want to retrieve the hyperlinks
     * @return a List of xml-Elements, or null if no links are available
     */
    public List<Element> getAttachments(int pos) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry) return null;
        // retrieve list of attachments
        List<Element> dummy = entry.getChild(ELEMENT_ATTACHMENTS).getChildren();
        List<Element> attachments = new LinkedList<Element>();
        // we have to manually copy all elements from one list to the other,
        // so we don't change the original content.
        Iterator<Element> it = dummy.iterator();
        // go through list
        while (it.hasNext()) {
            // retrieve element
            Element att = it.next();
            // change separator chars
            String attstring = Tools.convertSeparatorChars(att.getText(), settings);
            // add element
            if (!attstring.isEmpty()) {
                // create new element
                Element e = new Element(ELEMENT_ATTCHILD);
                // set text
                e.setText(attstring);
                // add element to return-list
                attachments.add(e);
            }
        }
        // else return the child-elements of the links-element
        return attachments;
    }
    /**
     * 
     * @param pos
     * @return 
     */
    public boolean hasAttachments(int pos) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry) return false;
        // retrieve list of attachments
        List<Element> dummy = entry.getChild(ELEMENT_ATTACHMENTS).getChildren();
        return (dummy!=null && dummy.size()>0);
    }
    /**
     * 
     * @param pos
     * @return 
     */
    public boolean hasAuthors(int pos) {
        String[] aus = getAuthors(pos);
        return (aus!=null && aus.length>0);
    }
    /**
     * 
     * @param pos
     * @return 
     */
    public boolean hasKeywords(int pos) {
        String[] kws = getKeywords(pos);
        return (kws!=null && kws.length>0);
    }
    /**
     * 
     * @param pos
     * @return 
     */
    public boolean hasRemarks(int pos) {
        String rem = getRemarks(pos);
        return (!rem.isEmpty());
    }
    /**
     * This method returns the links of an entry. since we can have more than just one
     * link/hyperlink per entry, the return-value is string-arra y which contain the
     * links of an entry.
     *
     * @param pos the entry from which we want to retrieve the hyperlinks
     * @param makeLinkToAttachment {@code true} if the attachment should be linked, in case the attachment
     * isan existing file on the hard disk. in this case, the attachment is surrounded by "file://" references.
     * @return a string-array of links/attachments, or null if no links are available
     */
    public String[] getAttachmentsAsString(int pos, boolean makeLinkToAttachment) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry) return null;
        // get the child-elements of the links-element
        List<Element> links = entry.getChild(ELEMENT_ATTACHMENTS).getChildren();
        // create iterator and copy all elements to a linked list
        Iterator<Element> i = links.iterator();
        ArrayList<String> list = new ArrayList<String>();
        // copy list to array
        while (i.hasNext()) {
            // get each link-element
            Element e = i.next();
            // get link
            String link = e.getText();
            if (!link.isEmpty()) {
                // convert separator chars
                link = Tools.convertSeparatorChars(link, settings);
                // if the attachment should be linked, check whether it is an existing file
//                if (makeLinkToAttachment) {
                    // TODO hier noch weitermachen? Anhänge automatsch verlinken
//                    if (!CCommonMethods.isHyperlink(link)) {
//                        File linkfile = CCommonMethods.getLinkFile(settings, link);
//                        // convert all file-attachments to hyperlinks
//                        String file = "file://";
//                        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) file = File.separatorChar+file;
//                        link = "<a href=\""+file+linkfile.toString()+"\">"+link+"</a>";
//                    }
//                }
                list.add(link);
            }
        }
        return list.toArray(new String[list.size()]);
    }
    /**
     * This method sets the links of an entry.
     *
     * @param pos the entry from which we want to set/change the hyperlinks and attachments
     * @param attachments a string-array containing the hyperlinks, attachmentspaths etc.
     */
    public void setAttachments(int pos, String[] attachments) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry || null==attachments || attachments.length<1) return;
        // remove all existing links from that entry
        entry.getChild(ELEMENT_ATTACHMENTS).removeChildren(ELEMENT_ATTCHILD);
        // save modification-stata
        boolean mod = false;
        // add each hyperlink string
        // therefor, iterate the array
        for (String a : attachments) {
            try {
                // create a new subchuld-element
                Element sublink = new Element(ELEMENT_ATTCHILD);
                // add the link-string from the array
                sublink.setText(a);
                // and add sublink-element to entry's child's content
                entry.getChild(ELEMENT_ATTACHMENTS).addContent(sublink);
                // change modification state
                mod = true;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
            }
            catch (IllegalAddException ex) {
                Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
            }
        }
        // change modified state
        if (mod) setModified(true);
    }


    /**
     * This method add the links to an entry.
     *
     * @param pos the entry from which we want to set/change the hyperlinks and attachments
     * @param attachments a string-array containing the hyperlinks, attachmentspaths etc.
     */
    public void addAttachments(int pos, String[] attachments) {
        // retrieve the entry
        Element entry = retrieveElement(zknFile, pos);
        // if no element exists, return empty array
        if (null==entry || null==attachments || attachments.length<1) return;
        // save modification-stata
        boolean mod = false;
        // add each hyperlink string
        // therefor, iterate the array
        for (String a : attachments) {
            try {
                // create a new subchuld-element
                Element sublink = new Element(ELEMENT_ATTCHILD);
                // add the link-string from the array
                sublink.setText(a);
                // and add sublink-element to entry's child's content
                entry.getChild(ELEMENT_ATTACHMENTS).addContent(sublink);
                // change modification state
                mod = true;
            }
            catch (IllegalDataException ex) {
                Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
            }
            catch (IllegalAddException ex) {
                Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
            }
        }
        // change modified state
        if (mod) setModified(true);
    }


    /**
     * This method changes attachment-values of an entry.
     *
     * @param oldAttachment the old attachment-value of the entry {@code entrynr} before it was changed
     * @param newAttachment the new value that should replace {@code oldAttachment}
     * @param entrynr the number of the entry which attachment should be changed
     */
    public void changeAttachment(String oldAttachment, String newAttachment, int entrynr) {
        // get links of entry
        List<Element> oldlinks = getAttachments(entrynr);
        // if we have any, we can go on...
        if (oldlinks!=null && oldAttachment!=null && newAttachment!=null) {
            // create linked list that will contain the updated attachments
            List<String> attachments = new ArrayList<String>();
            // iterator for current attachments of the entry
            Iterator<Element> i = oldlinks.iterator();
            // go...
            while (i.hasNext()) {
                // retrieve each attachment as element
                Element e = i.next();
                // get attachment-value
                String currentattachment = e.getText();
                // if attachment-value equals the old value, replace it with the new value
                if (currentattachment.equals(oldAttachment)) currentattachment = newAttachment;
                // add attachment to linked list
                attachments.add(currentattachment);
            }
            // set links back to the entry
            setAttachments(entrynr,attachments.toArray(new String[attachments.size()]));
        }
    }


    /**
     * This method deletes an attachment-value of an entry.
     *
     * @param value the attachment-value that should be removed from the entry {@code entrynr}
     * @param entrynr the number of the entry which attachment should be changed
     */
    public void deleteAttachment(String value, int entrynr) {
        // get links of entry
        List<Element> oldlinks = getAttachments(entrynr);
        // if we have any, we can go on...
        if (oldlinks!=null) {
            // create linked list that will contain the updated attachments
            List<String> attachments = new ArrayList<String>();
            // iterator for current attachments of the entry
            Iterator<Element> i = oldlinks.iterator();
            // go...
            while (i.hasNext()) {
                // retrieve each attachment as element
                Element e = i.next();
                // get attachment-value
                String currentattachment = e.getText();
                // if attachment-value does not equals the delete-value, add attachment-value to list
                if (!currentattachment.equals(value)) attachments.add(currentattachment);

            }
            // set links back to the entry
            setAttachments(entrynr,attachments.toArray(new String[attachments.size()]));
            // change up-to-date-value
            setAttachmentlistUpToDate(false);
        }
    }


    /**
     * This method returns the remarks of a given entry. The entry-number which
     * remarks should be retrieved, is passed via paramter (pos).
     * 
     * @param pos (the entry from which we want to retrieve the hyperlinks)
     * @return a string containing the remarks of an entry, or an empty string if no remarks found
     */
    public String getRemarks(int pos) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_REMARKS)) return "";
        // else return remarks
        return el.getChild(ELEMENT_REMARKS).getText();
    }
    /**
     * This method changes the remarks of a given entry with the entry-number {@code pos}.
     *
     * @param pos the entry from which we want to change the remarks
     * @param remarks the new remarks-content
     * @return {@code true} if remarks have been successfully changed, false otherwise
     */
    public boolean setRemarks(int pos, String remarks) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return false
        if (null==el || null==el.getChild(ELEMENT_REMARKS)) return false;
        try {
            // else change remarks
            el.getChild(ELEMENT_REMARKS).setText(remarks);
            // change modified state
            setModified(true);
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            return false;
        }
        // and tell about success.
        return true;
    }
    /**
     * This method returns the cleaned remarks of a given entry, i.e. [br]-tags converted to
     * new lines. The entry-number which
     * remarks should be retrieved, is passed via paramter {@code pos}.
     *
     * @param pos the entry from which we want to retrieve the remarks
     * @return a string containing the cleaned remarks of an entry (i.e. [br]-tags converted to new
     * lines), or an empty string if no remarks found
     */
    public String getCleanRemarks(int pos) {
        String content = getRemarks(pos);
        if (!content.isEmpty()) {
            content = content.replace("[br]", System.getProperty("line.separator"));
        }
        // return the cleaned string
        return content;
    }

    
    /**
     * This method retrieves all keywords of the currently <i>activated</i> entry
     * 
     * @return a string array with all keywords of the current <i>activated</i> entry
     */
    public String[] getCurrentKeywords() {
        return getKeywords(zettelPos);
    }
    
    
    /**
     * This method retrieves all keywords of a given entry.
     * 
     * <b>Caution!</b> The position {@code pos} is a value <i>from 1 to (size of zknfile)</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1) - , so we can directly
     * use the index number which are displayed in the jTable of the main window. However,
     * the access to the xml files are ranged between 0 and size-1, but this is achieved
     * in the retrieveElement-method, where we use "pos-1" to locate the correct entry
     *
     * @param pos a value from 1 to (size of zknfile), indicating the entry-number of which keywords are requested
     * @return a string array with all keywords of the requested entry, or <i>null</i> if no
     * keywords were found.
     */
    public String[] getKeywords(int pos) {
        // first retrieve the current "zettel" element
        Element kw = retrieveElement(zknFile,pos);
        // if no element exist, return failed value
        if (null==kw) return null;
        // if no keyword index numbers exist, return failed value
        if (kw.getChild(ELEMENT_KEYWORD).getText().isEmpty()) return null;
        // then get the keyword indexnumbers
        String[] kwa = kw.getChild(ELEMENT_KEYWORD).getText().split(",");
        // create a new string array return value, which will contain the keyword strings
        String[] retval = new String[kwa.length];
        // iterate the array
        // convert each keyword index number into an integer value
        // and get the related keyword string from the keyword data file
        // (this is achieved by the getKeyword-Method)
        for (int cnt=0;cnt<kwa.length;cnt++) {
            retval[cnt] = getKeyword(Integer.parseInt(kwa[cnt]));
        }
        return retval;
    }
    /**
     * This method retrieves all keywords of a given entry.
     *
     * <b>Caution!</b> The position {@code pos} is a value <i>from 1 to (size of zknfile)</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1) - , so we can directly
     * use the index number which are displayed in the jTable of the main window. However,
     * the access to the xml files are ranged between 0 and size-1, but this is achieved
     * in the retrieveElement-method, where we use "pos-1" to locate the correct entry
     *
     * @param pos a value from 1 to (size of zknfile), indicating the entry-number of which keywords are requested
     * @param sort {@code true} if keywords should be sorted alphabetically, {@code false} otherwise
     * @return a string array with all keywords of the requested entry, or <i>null</i> if no
     * keywords were found.
     */
    public String[] getKeywords(int pos, boolean sort) {
        // retrieve entry's keywords
        String[] kws = getKeywords(pos);
        // if we have any, sort them
        if (kws!=null && kws.length>0) {
            // sort array if requested
            Arrays.sort(kws, new Comparer());
            return kws;
        }
        // else return null
        else return null;
    }
    /**
     * This method retrieves all keywords of a given entry, but separates single keywords that consists of
     * several words. For instance, comma or divis-separated phrases in one keyword, would be split into single
     * parts. i.e. <b>Zettelkasten, programme</b> would be split into two return values: <b>Zettelkasten</b> and
     * <b>programme</b>. This method migh be useful for highlighting keywords...
     *
     * <b>Caution!</b> The position {@code pos} is a value <i>from 1 to (size of zknfile)</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1) - , so we can directly
     * use the index number which are displayed in the jTable of the main window. However,
     * the access to the xml files are ranged between 0 and size-1, but this is achieved
     * in the retrieveElement-method, where we use "pos-1" to locate the correct entry
     *
     * @param pos a value from 1 to (size of zknfile), indicating the entry-number of which keywords are requested
     * @return a string array with all keywords of the requested entry, or <i>null</i> if no
     * keywords were found.
     */
    public String[] getSeparatedKeywords(int pos) {
        // return separated keywords...
        return Tools.retrieveSeparatedKeywords(getKeywords(pos),false);
    }
    /**
     * This method retrieves all authors of a given entry.
     * 
     * <b>Caution!</b> The position {@code pos} is a value <i>from 1 to (size of zknfile)</i> - in contrary
     * to usual array handling where the range is from 0 to (size-1) - so we can directly
     * use the index number which are displayed in the jTable of the main window. However,
     * the access to the xml files are ranged between 0 and size-1, but this is achieved
     * in the retrieveElement-method, where we use "pos-1" to locate the correct entry
     *
     * @param pos a value from 1 to (size of zknfile), indicating the entrynumber of the entry which authors are requested
     * @return a string array with all authors of the requested entry, or <i>null</i> if no author was found
     */
    public String[] getAuthors(int pos) {
        // first retrieve the current "zettel" element
        Element au = retrieveElement(zknFile,pos);
        // if no element exist, return failed value
        if (null==au) return null;
        // if no author index numbers exist, return failed value
        if (au.getChild(ELEMENT_AUTHOR).getText().isEmpty()) return null;
        // then get the author indexnumbers
        String[] aunr = au.getChild(ELEMENT_AUTHOR).getText().split(",");
        // create a new string array return value, which will contain the author strings
        String[] retval = new String[aunr.length];
        // iterate the array
        // convert each authorindex number into an integer value
        // and get the related keyword string from the author data file
        // (this is achieved by the getAuthor-Method)
        for (int cnt=0;cnt<aunr.length;cnt++) retval[cnt] = getAuthor(Integer.parseInt(aunr[cnt]));
        return retval;
    }
    /**
     * This method returns the size of one of the xml data files. Following constants should
     * be used as parameters:<br>
     * ZKNCOUNT<br>
     * KWCOUNT<br>
     * AUCOUNT<br>
     * 
     * @param what (uses constants, see global field definition at top of source)
     * @return the size of the requested data file
     */
    public int getCount(int what) {
        Document doc;
        // check which file to count
        switch (what) {
            case ZKNCOUNT: doc = zknFile; break;
            case KWCOUNT: doc = keywordFile; break;
            case AUCOUNT: doc = authorFile; break;
            default: doc = zknFile; break;
        }
        // return XML file size
        return doc.getRootElement().getContentSize();
    }
    /**
     * This method adds the new zettel-position to the history, so the user
     * can go back and fore to previous selected entries.
     */
    private void addToHistory() {
        addToHistory(zettelPos);
    }
    /**
     * This method adds the entry-number {@code entrynr} to the history, so the user
     * can go back and fore to previous selected entries.
     * 
     * @param entrynr the number of the entry that should be added to the history
     */
    public void addToHistory(int entrynr) {
        // when the last history-entry equals the current entry, don't add
        // that to the history, so we don't have the same entry several times
        if (history[historyPosition]==entrynr) return;
        // when we reached the end of the array, rotate it...
        if (historyPosition>=(HISTORY_MAX-1)) {
            // go through history array...
            // copy the next element the previous position
            for (int cnt=0; cnt<(HISTORY_MAX-1); cnt++) history[cnt] = history[cnt+1];
            // add new value to history
            history[HISTORY_MAX-1] = entrynr;
            // set position and counter
            historyCount = HISTORY_MAX;
            historyPosition = HISTORY_MAX-1;
        }
        else {
            // in any other case, simply increase the history counter
            historyPosition++;
            // add the new value
            history[historyPosition] = entrynr;
            // and set the internal counter.
            historyCount = historyPosition+1;
        }
    }
    /**
     * Indicates whether the history-back function is possible or not.
     * @return {@code true}, if the histor-back-function is enabled, false otherwise
     */
    public boolean canHistoryBack() {
        return (historyPosition>0);
    }
    /**
     * Indicates whether the history-fore function is possible or not.
     * @return {@code true}, if the histor-fore-function is enabled, false otherwise
     */
    public boolean canHistoryFore() {
        return (historyPosition<(historyCount-1));
    }
    /**
     * This methods goes back through the history and sets the current entry
     * to the related entry in the history...
     */
    public void historyBack() {
        // check whether we can go back through history
        if (historyPosition>0) {
            // if yes, decrease history position counter
            historyPosition--;
            // and set new zettel-position
            zettelPos = history[historyPosition];
        }
    }
    /**
     * This methods goes fore through the history and sets the current entry
     * to the related entry in the history...
     */
    public void historyFore() {
        // check whether we can go fore through history
        if (historyPosition<(historyCount-1)) {
            // if yes, increase history position counter
            historyPosition++;
            // and set new zettel-position
            zettelPos = history[historyPosition];
        }
    }
    /**
     * This method sets the index for the currently displayed entry
     * to a given number. With this method, we can directly go to
     * a certain entry. The entry itself is displayed via the "updateDisplay" method
     * from the main frame
     * 
     * @param nr the number of the entry which should be displayed
     * @return 
     */
    public boolean gotoEntry(int nr) {
        // check whether it's out of bounds
        // and leave method if it is...
        if (!zettelExists(zettelPos) || isDeleted(nr)) return false;
        // else set the counter for the currently displayed entry
        zettelPos=nr;
        // update History
        addToHistory();
        // and give positive feedback
        return true;
    }
    /**
     * This methods increases the counter of the currently displayed entry
     */
    public void nextEntry() {
        // increase counter for currently display entry
        zettelPos = getNextZettel(zettelPos);
        // check whether it's out of bounds
        if (!zettelExists(zettelPos)) zettelPos = getFirstZettel();
        // update History
        addToHistory();
    }
    /**
     * This methods decreases the counter of the currently displayed entry
     */
    public void prevEntry() {
        // decrease counter for currently display entry
        zettelPos = getPrevZettel(zettelPos);
        // check whether it's out of bounds
        if (!zettelExists(zettelPos)) zettelPos = getLastZettel();
        // update History
        addToHistory();
    }
    /**
     * This methods sets the counter of the currently displayed entry to the first entry
     */
    public void firstEntry() {
        // set counter for currently display entry to 1
        zettelPos=getFirstZettel();
        // update History
        addToHistory();
    }
    /**
     * This methods sets the counter of the currently displayed entry to the last entry
     * in the data file
     */
    public void lastEntry() {
        // set counter for currently display entry to last element
        zettelPos=getLastZettel();
        // update History
        addToHistory();
    }
    /**
     * This method returns the index numbers of an entry's keywords as an integer array
     * This method is used for creating the links (connection between entries based on
     * matching keywords), which are displayed in a table on the JTabbedPane of the main window
     * 
     * @param pos (the entry's number)
     * @return an array of integer values (the keyword index numbers of the requested entry), or
     * null if no keywords exist...
     */
    public int[] getKeywordIndexNumbers(int pos) {
        // first retrieve the current "zettel" element
        Element dummy = retrieveElement(zknFile,pos);
        // if no element found, return failed value
        if (null==dummy) return null;
        // if no keyword index numbers exist, return failed value
        if (dummy.getChild(ELEMENT_KEYWORD).getText().isEmpty()) return null;
        // then get the keyword indexnumbers
        String[] kwa = dummy.getChild(ELEMENT_KEYWORD).getText().split(",");
        // create a new string array return value, which will contain the keyword strings
        int[] retval = new int[kwa.length];
        // iterate the array
        // convert each keyword index number into an integer value
        // and get the related keyword string from the keyword data file
        // (this is achieved by the getKeyword-Method)
        for (int cnt=0;cnt<kwa.length;cnt++) retval[cnt] = Integer.parseInt(kwa[cnt]);
        return retval;
    }
    /**
     * This method sets the index numbers of an entry's keywords,
     * the entry's reference to keyword values will be set.
     * <br><br>
     * This method does not affect the keyword-xml-file.
     * 
     * @param pos (the entry-number of the entry, which keywords should be changed)
     * @param kws (a string with the keyword-index-numbers, separated by commas)
     */
    public void setKeywordIndexNumbers(int pos, String kws) {
        setIndexNumbers(ELEMENT_KEYWORD, pos, kws);
    }
    /**
     * This method sets the index numbers of an entry's keywords or authors,
     * the entry's reference to keyword or author values will be set.
     * <br><br>
     * This method does neither affect the keyword- nor the author-xml-file.
     * 
     * @param attr the attribut of which element should be changed. use either
     * {@link #ELEMENT_AUTHOR} or {@link #ELEMENT_KEYWORD}.
     * @param pos the entry-number of the entry, which authors/keywords should be changed
     * @param values a string with the keyword/author-index-numbers, separated by commas
     */
    private void setIndexNumbers(String attr, int pos, String values) {
        // first retrieve the current "zettel" element
        Element dummy = retrieveElement(zknFile, pos);
        // if no element found, return failed value
        if (null==dummy) return;
        // set new keyword-index-numbers
        dummy.getChild(attr).setText(values);
        // change modified state
        setModified(true);
    }
    /**
     * This method sets the index numbers of an entry's author, i.e.
     * the entry's reference to author values will be set.
     * <br><br>
     * This method does not affect the author-xml-file.
     * 
     * @param pos the entry-number of the entry, which authors should be changed
     * @param aus a string with the author-index-numbers, separated by commas
     */
    public void setAuthorIndexNumbers(int pos, String aus) {
        setIndexNumbers(ELEMENT_AUTHOR, pos, aus);
    }
    /**
     * This method returns the index-numbers of an entry's authors as an integer value
     * 
     * @param pos (the entry's number)
     * @return an integer array (the index numbers of the requested entry's authors), or null
     * if no author index numbers exist
     */
    public int[] getAuthorIndexNumbers(int pos) {
        // first retrieve the current "zettel" element
        Element dummy = retrieveElement(zknFile,pos);
        // if no element found, return failed value
        if (null==dummy) return null;
        // if no author index numbers exist, return failed value
        if (dummy.getChild(ELEMENT_AUTHOR).getText().isEmpty()) return null;
        // then get the autors indexnumbers
        String[] aun = dummy.getChild(ELEMENT_AUTHOR).getText().split(",");
        // create a new string array return value, which will contain the authors strings
        int[] retval = new int[aun.length];
        // iterate the array
        // convert each author index number into an integer value
        for (int cnt=0;cnt<aun.length;cnt++) retval[cnt] = Integer.parseInt(aun[cnt]);
        return retval;
    }
    /**
     * This method counts the frequency (amount of appearance) of a keyword in the
     * main data file. Therefor, pass the index number of the keyword which has to be found.
     * After that, we iterate the main zkndata file, retrieving the keyword index numbers of 
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     * 
     * @param pos (the index number of the keyword which you are looking for)
     * @return (the amount of appearance / frequency of this keyword in the main data file)
     */
    public int getKeywordFrequencies(int pos) {
        int retval = 0;
        // go through all entrys of the main data file (zknfile)
        for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
            // get the keyword index numbers of each entry
            int[] kwn = getKeywordIndexNumbers(cnt);
            // check whether we have any keywords at all
            if ((kwn!=null)&&(kwn.length>0)) {
                // iterate the keyword index numbers of each entry
                for (int val : kwn) {
                    // if a keyword index number matches the requested keyword
                    // increase the keyword counter
                    if (val == pos) {
                        retval++;
                        break;
                    }
                }
            }
        }
        return retval;
    }
    /**
     * This method counts the frequency (amount of appearance) of a keyword in the
     * main data file. Therefor, pass the index number of the keyword which has to be found.
     * After that, we iterate the main zkndata file, retrieving the keyword index numbers of 
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     * 
     * @param kw the keyword of which the frequency should be counted
     * @return the amount of appearance / frequency of this keyword in the main data file
     */
    public int getKeywordFrequencies(String kw) {
        int retval = 0;
        // go through all entrys of the main data file (zknfile)
        for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
            // check whether we have any keywords at all
            if (existsInKeywords(kw, cnt, true)) retval++;
        }
        return retval;
    }
    /**
     * This method counts the frequency (amount of appearance) of a keyword in the
     * main data file. Therefor, pass the index number of the keyword which has to be found.
     * After that, we iterate the main zkndata file, retrieving the keyword index numbers of
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     *
     * @param pos (the index number of the keyword which you are looking for)
     * @return (the amount of appearance / frequency of this keyword in the main data file)
     */
    public int getKeywordFrequency(int pos) {
        // retrieve the keyword element
        Element keyword = retrieveElement(keywordFile, pos);
        // check whether element is null
        if (null==keyword) {
            return 0;
        }
        else {
            try {
                String freq = keyword.getAttributeValue(ATTRIBUTE_FREQUENCIES);
                if (freq!=null) {
                    return Integer.parseInt(freq);
                }
                else {
                    return 0;
                }
            }
            catch (NumberFormatException ex) {
                return 0;
            }
        }
    }
    /**
     * This method counts the frequency (amount of appearance) of an author in the
     * main data file. Therefor, pass the index number of the author which has to be found.
     * After that, we iterate the main zkndata file, retrieving the author index numbers of 
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     * 
     * @param pos the index number of the author which you are looking for
     * @return the amount of appearance / frequency of this author in the main data file
     */
    public int getAuthorFrequencies(int pos) {
        int retval = 0;
        
        // go through all entrys of the main data file (zknfile)
        for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
            // get the author index numbers of each entry
            int[] aun = getAuthorIndexNumbers(cnt);
            // check whether we have any author index numbers at all
            if ((aun!=null)&&(aun.length>0)) {
                // iterate the author index numbers of each entry
                for (int val : aun) {
                    // if an author index number matches the requested author
                    // increase the keyword counter
                    if (val == pos) {
                        retval++;
                        break;
                    }
                }
            }
        }
        return retval;
    }
    /**
     * This method counts the frequency (amount of appearance) of an author in the
     * main data file. Therefor, pass the index number of the author which has to be found.
     * After that, we iterate the main zkndata file, retrieving the author index numbers of 
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     * 
     * @param au the author-value of which the frequency should be counted
     * @return the amount of appearance / frequency of this author in the main data file
     */
    public int getAuthorFrequencies(String au) {
        int retval = 0;
        // go through all entrys of the main data file (zknfile)
        for (int cnt=1; cnt<=getCount(ZKNCOUNT);cnt++) {
            // check whether we have any keywords at all
            if (existsInAuthors(au, cnt)) retval++;
        }
        return retval;
    }
    /**
     * This method counts the frequency (amount of appearance) of a keyword in the
     * main data file. Therefor, pass the index number of the keyword which has to be found.
     * After that, we iterate the main zkndata file, retrieving the keyword index numbers of
     * each entry, and then look for the appearance of "pos" (the given index number). Each time
     * we find that index number in an entry, the counter for the total frequency is increased by one.
     *
     * @param pos the index number of the author which you are looking for
     * @return the amount of appearance / frequency of this author in the main data file
     */
    public int getAuthorFrequency(int pos) {
        // retrieve the keyword element
        Element author = retrieveElement(authorFile, pos);
        // check whether element is null
        if (null==author) return 0;
        else return Integer.parseInt(author.getAttributeValue(ATTRIBUTE_FREQUENCIES));
    }
    /**
     * This method returns the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.<br><br>
     * This attribute is optional, so {@code null} might be returned.
     *
     * @param pos the index number of the author which you are looking for. Remember that this value
     * has a range from 1 to {@link #getCount(int) getCount(AUCOUNT)}.
     * @return a string containing the {@code bibkey} string of the author. if no such attribute or no
     * such author-element exists, {@code null} is returned. if attribute is empty, an empty string
     * is returned.
     */
    public String getAuthorBibKey(int pos) {
        return getAuthorBibKeyValue(pos);
    }
    /**
     * This method returns the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.<br><br>
     * This attribute is optional, so {@code null} might be returned.
     *
     * @param au the author-value as string
     * @return a string containing the {@code bibkey} string of the author. if no such attribute or no
     * such author-element exists, {@code null} is returned. if attribute is empty, an empty string
     * is returned.
     */
    public String getAuthorBibKey(String au) {
        return getAuthorBibKeyValue(getAuthorPosition(au));
    }
    /**
     * This method returns the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.<br><br>
     * This attribute is optional, so {@code null} might be returned.<br><br>
     * This method does the work for both
     * {@link #getAuthorBibKey(java.lang.String) getAuthorBibKey(String)} and
     * {@link #getAuthorBibKey(int) getAuthorBibKey(int)}.
     *
     * @param pos the index number of the author which you are looking for. Remember that this value
     * has a range from 1 to {@link #getCount(int) getCount(AUCOUNT)}.
     * @return a string containing the {@code bibkey} string of the author. if no such attribute or no
     * such author-element exists, {@code null} is returned. if attribute is empty, an empty string
     * is returned.
     */
    private String getAuthorBibKeyValue(int pos) {
        // if we have no such author, return null
        if (-1==pos || pos>getCount(AUCOUNT)) return null;
        // retrieve the keyword element
        Element author = retrieveElement(authorFile, pos);
        // check whether element is null
        if (null==author) return null;
        // else return the attribute-value
        return author.getAttributeValue(ATTRIBUTE_AUTHOR_BIBKEY);
    }
    /**
     * This method sets the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.
     *
     * @param pos the index number of the author which you are looking for
     * @param key the bibkey of the related BibTex-entry.
     * @return {@code true} if bibkey-attribute was successfully changed, {@code false} if an error occured
     */
    public boolean setAuthorBibKey(int pos, String key) {
        return setAuthorBibKeyValue(pos,key.trim());
    }
    /**
     * This method sets the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.
     *
     * @param au the author-value as string of that author where the bibkey-value should be changed
     * @param key the bibkey of the related BibTex-entry.
     * @return {@code true} if bibkey-attribute was successfully changed, {@code false} if an error occured
     */
    public boolean setAuthorBibKey(String au, String key) {
        // check for valid values
        if (null==au || null==key || au.isEmpty()) {
            return false;
        }
        // retrieve author-position
        int pos = getAuthorPosition(au);
        // if we have no such author, return null
        if (-1==pos) return false;
        return setAuthorBibKeyValue(pos,key.trim());
    }
    /**
     * This method sets the bibkey-string of an author-value. the bibkey-string referres to a
     * BibTex-entry in a given BibTex-file, so the "formatted" author of the author-value saved
     * in our authorXml-file can be retrieved via a BibTex-File.<br><br>
     * This method does the work for both
     * {@link #setAuthorBibKey(java.lang.String, java.lang.String) setAuthorBibKey(String, String)}
     * and {@link #setAuthorBibKey(int, java.lang.String) setAuthorBibKey(int, String)}.
     *
     * @param pos the index number of the author which you are looking for
     * @param key the bibkey of the related BibTex-entry.
     * @return {@code true} if bibkey-attribute was successfully changed, {@code false} if an error occured
     */
    private boolean setAuthorBibKeyValue(int pos, String key) {
        // retrieve the keyword element
        Element author = retrieveElement(authorFile, pos);
        // check whether element is null
        if (null==author) return false;
        try {
            // if everything ok, set new attribute-value
            author.setAttribute(ATTRIBUTE_AUTHOR_BIBKEY, key);
            // and change modified-state
            setModified(true);
        }
        catch (IllegalNameException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            return false;
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
            return false;
        }
        return true;
    }
    /**
     * This method sets the current entry, that means the last activated entry before closing 
     * the program. With this, we can show the last shown entry on startup of the program.
     * <br><br>
     * This method should be called directly before closing a data-file!
     * @param nr
     */
    public void setCurrentZettelPos(int nr) {
        zettelPos=nr;
    }
    /**
     * This method sets the initial history value. This method should be used when
     * the startup-entry or a random-startup-entry is set.
     * @param nr
     */
    public void setInitialHistoryPos(int nr) {
        history[0] = nr;
    }
    /**
     * This method returns the number of the currently <i>activated</i> entry
     * @return number of the currently <i>activated</i> entry
     */
    public int getCurrentZettelPos() {
        // in case, the position-index for the displayed to be displayed is out
        // of the valid boundaries, simply reset the position-index to 1
        if (!zettelExists(zettelPos)) zettelPos=getFirstZettel();
        // and return the value
        return zettelPos;
    }
    /**
     * This method calcualtes the relevance or strength of the connection of two entries, given
     * by their keywords. the more keywords of the {@code sourceentry} also are keywords
     * of the {@code destentry}, the higher the strength of the connection is. The maximum value is
     * 100%, i.e. each keyword of {@code sourceentry} is also a keyword of {@code destentry}.<br><br>
     * The strength is returned as a ratio, saying how many percent of {@code sourceentry}'s keywords
     * are also keywords of {@code destentry}.
     *
     * @param sourceentry the source entry, usually that one that currently is being displayed, which
     * is used as base for calculating the connection-ration
     * @param destentry one of the entries that is connected with the {@code sourceentry} via
     * identical keywords.
     * @return the strength of the entry-connecion, an integer-ratio with a maximum of 100% (i.e.
     * the return-value is an integer-value ranged from 0 to 100), saying how many percent of
     * {@code sourceentry}'s keywords are also keywords of {@code destentry}.
     */
    public int getLinkStrength(int sourceentry, int destentry) {
        // check for valid parameters
        if (sourceentry<0 || sourceentry>getCount(ZKNCOUNT) || destentry<0 || destentry>getCount(ZKNCOUNT)) return 0;
        // retrieve all keywords of source-entry
        String[] kws = getKeywords(sourceentry);
        // check for valid values
        if (null==kws || kws.length<1) return 0;
        // init counter for amount of identical keywords
        int keycnt = 0;
        // go through all keywords of the source-entry and check for existence
        for (String k :kws) {
            // check for existens of each keyword, and increase counter if necessary
            if (existsInKeywords(k, destentry, false)) keycnt++;
        }
        // calculate ratio
        int keylen = kws.length;
        double ratio = (double) 100.0 * keycnt / keylen;
        // return result
        return (int)ratio;
    }
    /**
     * This method returns the title of a certain entry
     * 
     * @param pos the index number of the entry which title is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @return the title of the requested entry as a string, or an empty string if an error occured
     */
    public String getZettelTitle(int pos) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_TITLE)) return "";
        // else return title
        return el.getChild(ELEMENT_TITLE).getText();
    }
    /**
     * This method returns the title of a certain entry
     * 
     * @param pos the index number of the entry which title is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @param title
     */
    public void setZettelTitle(int pos, String title) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_TITLE)) return;
        try {
            // else change title
            el.getChild(ELEMENT_TITLE).setText(title);
            // reset title-list
            setTitlelistUpToDate(false);
            // change modified state
            setModified(true);
        }
        catch (IllegalDataException ex) {
            Constants.zknlogger.log(Level.SEVERE,ex.getLocalizedMessage());
        }
    }
    /**
     * This method changes the "edited" timestamp of the entry with the index number
     * {@code pos}. By this, you can change the edited-date from an entry manually, if necessary.
     * The timestamp uses the current date, retrieved from {@code CCommonMethods.getTimestamp()}
     *
     * @param pos the entry's index-number of that entry which edited-timestamp should be changed.
     * {@code pos} has to be a value from 1 to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     */
    public void changeEditTimeStamp(int pos) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el) return;
        // set timestamp
        setTimestampEdited(el, Tools.getTimeStamp());
    }
    /**
     * This method returns the content of a certain entry, i.e.
     * the main entry text (text excerpt or whatever). The content
     * is returned as it is stored in the XML-datafile. So we have
     * the "plain text" here, <i>with</i> format-tags, but <i>not</i> prepared
     * for HTML-display.<br><br>
     * Use {@link #getEntryAsHtml(int, java.lang.String[]) getEntryAsHtml()}
     * if you need the HTML-formatted entry instead.<br><br>
     * Use {@link #getCleanZettelContent(int) getCleanZettelContent()} if you need
     * the plain text entry <i>without</i> format-tags.
     *
     * @param pos the index number of the entry which content is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @return the plain, non-html-converted content of the requested entry as a string
     * or an empty string if no entry was found or the requested entry does not exist
     */
    public String getZettelContent(int pos) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_CONTENT)) return "";
        // else return title
        return el.getChild(ELEMENT_CONTENT).getText();
    }
    /**
     * This method returns the content of a certain entry, i.e.
     * the main entry text (text excerpt or whatever). The content
     * is returned in HTML-format.<br><br>
     * Use {@link #getCleanZettelContent(int) getCleanZettelContent()} if you need
     * the plain text entry <i>without</i> format-tags.
     *
     * @param pos the index number of the entry which content is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @return the html-converted content of the requested entry as a string
     * or an empty string if no entry was found or the requested entry does not exist
     */
    public String getZettelContentAsHtml(int pos) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_CONTENT)) return "";
        // else return entry as html
        return HtmlUbbUtil.convertUbbToHtml(settings, this, bibtexObj, el.getChild(ELEMENT_CONTENT).getText(), Constants.FRAME_MAIN, false, false);
    }
    /**
     * This method returns the content of a certain entry, i.e.
     * the main entry text (text excerpt or whatever). The content
     * is returned as it is stored in the XML-datafile. So we have
     * the "plain text" here, <i>with</i> format-tags, but <i>not</i> prepared
     * for HTML-display.<br><br>
     * However, you can encode Unicode chars into its equivalent HTML entities
     * nby setting the parameter {@code encodeUTF} to {@code true}. This is
     * necessary when exporting entries to HTML or PDF.
     * <br><br>
     * Use {@link #getEntryAsHtml(int, java.lang.String[]) getEntryAsHtml()}
     * if you need the HTML-formatted entry instead.<br><br>
     * Use {@link #getCleanZettelContent(int) getCleanZettelContent()} if you need
     * the plain text entry <i>without</i> format-tags.
     *
     * @param pos the index number of the entry which content is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @param encodeUTF if {@code true}, unicode characters are encoded to the equivalent
     * HTML entities.
     * @return the plain, non-html-converted content of the requested entry as a string
     * or an empty string if no entry was found or the requested entry does not exist
     */
    public String getZettelContent(int pos, boolean encodeUTF) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_CONTENT)) return "";
        // retrieve entry's content
        String preparestring = el.getChild(ELEMENT_CONTENT).getText();
        // create dummy-string-builder
        StringBuilder buf = new StringBuilder("");
        // iterate each char of the string
        for (int i=0; i<preparestring.length(); i++) {
            // retrieve char
            char c = preparestring.charAt(i);
            // if it's a normal char, append it...
            if ((int)c<160) {
                buf.append(c);
            } else {
                // else append entity of unicode-char
                buf.append("&#").append((int) c).append(";");
            }
        }
        // return converted string
        return buf.toString();
    }
    /**
     * This method sets the content of a certain entry, i.e.
     * the main entry text (text excerpt or whatever).
     *
     * @param pos the index number of the entry which content is requested. Must be a number from 1
     * to {@link #getCount(int) getCount(CDaten.ZKNCOUNT)}.
     * @param content the new content of the entry
     * @param changetimestamp {@code true} if the entry's modified-timestamp should be updated
     * @return {@code true} if content was successfully changed, false otherwise
     */
    public boolean setZettelContent(int pos, String content, boolean changetimestamp) {
        // retrieve the element from the main xml-file
        Element el = retrieveElement(zknFile,pos);
        // if element or child element is null, return empty string
        if (null==el || null==el.getChild(ELEMENT_CONTENT)) return false;
        // else set new content
        el.getChild(ELEMENT_CONTENT).setText(content);
        // and change timestamp
        if (changetimestamp) changeEditTimeStamp(pos);
        // change modified state
        setModified(true);
        // and tell about success...
        return true;
    }
    /**
     * This method returns the cleaned content of an entry. Usually, the getZettelContent()-method
     * contains also the formatting-tags (like [f] or [k], see CHtml-class for more details). Since
     * these tags may also appear inside a word or phrase, search-results may miss this "splitted"
     * word and don't recognize that entry as valied-search-hit.
     * <br><br>
     * Therefore, when we want to search through the content (see CStartSearch-class for more details)
     * we want to have a clean text, removing all formatting tags...
     * 
     * @param pos (the entry-index-number)
     * @return the cleaned content of that entry, with all formatting-tags removed
     */
    public String getCleanZettelContent(int pos) {
        // get the zettel content
        String content = getZettelContent(pos);
        // if the content is not empty...
        if (!content.isEmpty()) {
            // return the cleaned string
            return Tools.removeUbbFromString(content, true);
        }
        return "";
    }
    /**
     * This method sets the keyword-up-to-date-state. This is used when creating
     * the keyword list via "CShowKeywordListDialog". This background task only needs
     * to be executed once and then only again after changes have been made to the keyword list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @param val (the new state, whether the keywordlist is uptodate or not)
     */
    public void setKeywordlistUpToDate(boolean val) {
        keywordlistUpToDate = val;
    }
    /**
     * This method gets the keyword-up-to-date-state. The background task for creating the
     * keyword list in the tabbedpane of the main window only needs
     * to be executed when changes have been made to the keyword list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @return whether the keywordlist is uptodate or not
     */
    public boolean isKeywordlistUpToDate() {
        return keywordlistUpToDate;
    }
    /**
     * This method sets the cluster-up-to-date-state. This is used when creating
     * the cluster list. This rebuilding only needs
     * to be executed once and then only again after changes have been made to the keyword list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @param val the new state, whether the clusterlist is uptodate or not
     */
    public void setClusterlistUpToDate(boolean val) {
        clusterlistUpToDate = val;
    }
    /**
     * This method gets the cluster-up-to-date-state. The rebuilding of the 
     * cluster list in the tabbedpane of the main window only needs
     * to be executed when changes have been made to the keyword list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @return whether the clusterlist is uptodate or not
     */
    public boolean isClusterlistUpToDate() {
        return clusterlistUpToDate;
    }
    /**
     * This method sets the author-up-to-date-state. This is used when creating
     * the author list via "CShowAuthorListDialog". This background task only needs
     * to be executed once and then only again after changes have been made to the author list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @param val the new state, whether the authorlist is uptodate or not
     */
    public void setAuthorlistUpToDate(boolean val) {
        authorlistUpToDate = val;
    }
    /**
     * This method gets the author-up-to-date-state. The background task for creating the
     * author list in the tabbedpane of the main window only needs
     * to be executed when changes have been made to the author list.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @return whether the authorlist is uptodate or not
     */
    public boolean isAuthorlistUpToDate() {
        return authorlistUpToDate;
    }
    /**
     * This method sets the title-up-to-date-state. This is used when creating
     * the title list via "CShowTitleListDialog". This background task only needs
     * to be executed once and then only again after changes have been made to the entries.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @param val the new state, whether the titlelist is uptodate or not
     */
    public void setTitlelistUpToDate(boolean val) {
        titlelistUpToDate = val;
    }
    /**
     * This method gets the title-up-to-date-state. The background task for creating the
     * title list in the tabbedpane of the main window only needs
     * to be executed when changes have been made to the entries.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     * 
     * @return whether the titlelist is uptodate or not
     */
    public boolean isTitlelistUpToDate() {
        return titlelistUpToDate;
    }
    /**
     * This method sets the attachment-up-to-date-state. This is used when creating
     * the attachment list via "CShowAttachmentListDialog". This background task only needs
     * to be executed once and then only again after changes have been made to the entries.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     *
     * @param val the new state, whether the attachmentlist is uptodate or not
     */
    public void setAttachmentlistUpToDate(boolean val) {
        attachmentlistUpToDate = val;
    }
    /**
     * This method gets the attachment-up-to-date-state. The background task for creating the
     * attachment list in the tabbedpane of the main window only needs
     * to be executed when changes have been made to the entries.
     * Otherwise, switching tabs in the tabbedpane of the main window takes too long.
     *
     * @return whether the attachmentlist is uptodate (true) or not (false)
     */
    public boolean isAttachmentlistUpToDate() {
        return attachmentlistUpToDate;
    }
    /**
     * This method is used in the {@link de.danielluedecke.zettelkasten.tasks.export.ExportToZknTask} class
     * to prepare the entries that should be exported. This method converts entry-number-references
     * into the related entry-IDs using the {@link #getZettelID(int) getZettelID(int)} method.
     * 
     * @param entrynumbers
     * @return
     */
    public boolean createExportEntries(ArrayList<Integer> entrynumbers) {
        // check for valid parameter
        if (entrynumbers!=null && entrynumbers.size()>0) {
            // create "empty" XML JDom objects
            zknFileExport = new Document(new Element(DOCUMENT_ZETTELKASTEN));
            for (Integer entrynumber : entrynumbers) {
                // create new zettel element
                // and clone content from requested zettel to our element
                Element zettel = (Element) retrieveZettel(entrynumber).clone();
                // retrieve content of entry and convert all author footnotes, which
                // contain author-index-numbers, into the related author-IDs.
                String content = zettel.getChild(ELEMENT_CONTENT).getText();
                // check for footnotes
                int pos = 0;
                while (pos!=-1) {
                    // find the html-tag for the footnote
                    pos = content.indexOf(Constants.FORMAT_FOOTNOTE_OPEN, pos);
                    // if we found something...
                    if (pos!=-1) {
                        // find the closing quotes
                        int end = content.indexOf("]", pos+2);
                        // if we found that as well...
                        if (end!=-1) {
                            try {
                                // extract footnote-number
                                String fn = content.substring(pos+4, end);
                                // retrieve author ID from related footnote number
                                try {
                                    String authorID = getAuthorID(Integer.parseInt(fn));
                                    // replace author number with author ID inside footnote
                                    content = content.substring(0, pos+4) + authorID + content.substring(end);
                                }
                                catch (NumberFormatException ex) {
                                    // log error
                                    Constants.zknlogger.log(Level.WARNING,ex.getLocalizedMessage());
                                    Constants.zknlogger.log(Level.WARNING,"Could not convert author number into author ID!");
                                }
                            }
                            catch (IndexOutOfBoundsException ex) {
                            }
                            // and add it to the linked list, if it doesn't already exist
                            // set pos to new position
                            pos = end;
                        }
                        else pos = pos+4;
                    }
                }                
                // check for manual links
                pos = 0;
                while (pos!=-1) {
                    // find the html-tag for the manual link
                    pos = content.indexOf(Constants.FORMAT_MANLINK_OPEN, pos);
                    // if we found something...
                    if (pos!=-1) {
                        // find the closing quotes
                        int end = content.indexOf("]", pos+2);
                        // if we found that as well...
                        if (end!=-1) {
                            try {
                                // extract manual-link-number
                                String ml = content.substring(pos+3, end);
                                // retrieve author ID from related footnote number
                                try {
                                    String zetID = getZettelID(Integer.parseInt(ml));
                                    // replace manual link number with entry ID
                                    content = content.substring(0, pos+3) + zetID + content.substring(end);
                                }
                                catch (NumberFormatException ex) {
                                    // log error
                                    Constants.zknlogger.log(Level.WARNING,"Could not convert manual link number into related entry ID!");
                                }
                            }
                            catch (IndexOutOfBoundsException ex) {
                            }
                            // and add it to the linked list, if it doesn't already exist
                            // set pos to new position
                            pos = end;
                        }
                        else pos = pos+3;
                    }
                }                
                // set back changes
                zettel.getChild(ELEMENT_CONTENT).setText(content);
                //
                // here we change the entry's luhmann-numbers (trailing entries) and the
                // entry's manual links with the unique IDs
                //
                replaceAttributeNrWithID(zettel);
                // add each entry-element to the export-document
                zknFileExport.getRootElement().addContent(zettel);
            }
            // set first and last zettel
            zknFileExport.getRootElement().setAttribute(ATTRIBUTE_FIRST_ZETTEL, "");
            zknFileExport.getRootElement().setAttribute(ATTRIBUTE_LAST_ZETTEL, "");
            return true;
        }
        return false;
    }
    /**
     * This method returns the XML-document that has been prepared for exporting into the
     * Zettelkasten-fileformat.
     * 
     * @return XML-document that has been prepared for export
     */
    public Document retrieveExportDocument() {
        return zknFileExport;
    }
    /**
     * This method removes wrong placed edit-tags in the xml-file. this error occured in the
     * {@link #changeEditTimeStamp(int) changeEditTimeStamp()} method, where the edit-element,
     * child-element of the timestamp-element, was set as child of the zettel-element (and not its
     * child "timestamp"). This method tries to fix this error...
     */
    public void fixWrongEditTags() {
        // iterate all elements
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            // retrieve element
            Element zettel = retrieveZettel(cnt);
            // check for valid value
            if (zettel!=null) {
                // check whether element has a child named "edited". if so, it either
                // has to be moved as sub-child to the child-element timestamp, or removed
                // if "timestamp" already has an edit-element
                Element edited = zettel.getChild("edited");
                // only proceed, if wrong placed edited element exists
                if (edited!=null) {
                    // retrieve timestamp-element
                    Element timestamp = zettel.getChild("timestamp");
                    // check for valid value
                    if (timestamp!=null) {
                        // retrieve edited-timestamp
                        Element timestampedit = timestamp.getChild("edited");
                        // check whether edited-element exists
                        if (null==timestampedit) {
                            // if timestampedit is null, the element has no edited-element
                            // so we add the content of the wrong placed element as new edited-element
                            // create new edited element
                            Element ed = new Element("edited");
                            // add to timestamp
                            timestamp.addContent(ed);
                            // set content
                            ed.setText(edited.getText());
                        }
                        else {
                            // now we know that an edited-element already exists
                            // we now want to check whether the existing editing-timestamp
                            // is older than the value in the wrong placed edited-element,
                            // and if so, update the timestamp
                            if (timestamp.getText().compareTo(edited.getText())<0) timestampedit.setText(edited.getText());
                        }
                    }
                    // and remove wrong edited element
                    zettel.removeChild("edited");
                }
            }
        }
    }
    /**
     * This methods updates the data base when updating to versiob 3.3. Each entry element
     * receives a unique ID as XML-attribute value stored in the data base. The unique ID consists
     * of entry's edited timestamp, file name and entry number.
     */
    public void db_updateZettelIDs() {
        // iterate all elements
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            // retrieve element
            Element zettel = retrieveZettel(cnt);
            // check for valid value
            // and check whether entry already has an ID
            if (zettel!=null && !hasZettelID(cnt)) {
                // if not, set unique ID-attribute to entry
                // init variable
                StringBuilder id = new StringBuilder("");
                // retrieve timestamp
                String[] ts = getTimestamp(cnt);
                // check for valid entry
                if (ts!=null && ts[0]!=null && !ts[0].isEmpty()) {
                    // append timestamp
                    id.append(ts[0]);
                }
                else {
                    // else, if entry has no create-timestamp, add manual timestamp
                    id.append(Tools.getTimeStampWithMilliseconds());
                }
                id.append(String.valueOf(cnt)).append(settings.getFileName()).append(String.valueOf(cnt));
                // now add id to zettel-element
                zettel.setAttribute(ATTRIBUTE_ZETTEL_ID, id.toString());
            }
        }
        // change modified state
        setModified(true);
    }
    /**
     * This method updates the data base when updating from data base version 3.3 to 3.4.
     * Here each author and keyword element in the {@link #authorFile} and {@link #keywordFile}
     * get 2 new attributes: {@link #ATTRIBUTE_AUTHOR_TIMESTAMP} and {@link #ATTRIBUTE_AUTHOR_ID} (resp.
     * their keyword-equivalence).
     */
    public void db_updateAuthorAndKeywordIDs() {
        // iterate all elements
        for (int cnt=1; cnt<=getCount(AUCOUNT); cnt++) {
            // retrieve element
            Element author = retrieveElement(authorFile, cnt);
            // check for valid value
            // and check whether element already has an ID
            if (author!=null && !hasAuthorID(cnt)) {
                // if not, set unique ID-attribute to entry
                // init variable
                StringBuilder id = new StringBuilder("");
                // add manual timestamp
                id.append(Tools.getTimeStampWithMilliseconds()).append(settings.getFileName()).append(String.valueOf(cnt));
                // now add id to zettel-element
                author.setAttribute(ATTRIBUTE_AUTHOR_ID, id.toString());
                // and add timestamp attribute
                author.setAttribute(ATTRIBUTE_AUTHOR_TIMESTAMP, Tools.getTimeStampWithMilliseconds());
            }
        }
        // iterate all elements
        for (int cnt=1; cnt<=getCount(KWCOUNT); cnt++) {
            // retrieve element
            Element keyword = retrieveElement(keywordFile, cnt);
            // check for valid value
            // and check whether element already has an ID
            if (keyword!=null && !hasKeywordID(cnt)) {
                // if not, set unique ID-attribute to entry
                // init variable
                StringBuilder id = new StringBuilder("");
                // add manual timestamp
                id.append(Tools.getTimeStampWithMilliseconds()).append(settings.getFileName()).append(String.valueOf(cnt));
                // now add id to zettel-element
                keyword.setAttribute(ATTRIBUTE_KEYWORD_ID, id.toString());
                // and add timestamp attribute
                keyword.setAttribute(ATTRIBUTE_KEYWORD_TIMESTAMP, Tools.getTimeStampWithMilliseconds());
            }
        }
        // change modified state
        setModified(true);
    }
    /**
     * This method updates the timestamp-attributes in the data base. The former
     * XML-elements (timestamp) with the children "created" and "edited" are from database
     * version 3.4 on simply stored as attribute of each element.
     */
    public void db_updateTimestampAttributes() {
        // iterate all elements
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            // retrieve element
            Element zettel = retrieveZettel(cnt);
            // check for valid value
            if (zettel!=null) {
                // init strings with default values
                String created = Tools.getTimeStamp();
                String edited = "";
                // retrieve created-timestamp
                Element el = zettel.getChild("timestamp").getChild("created");
                if (el!=null) created = el.getText();
                // retrieve edited-timestamp
                el = zettel.getChild("timestamp").getChild("edited");
                if (el!=null) edited = el.getText();
                // remove old values
                zettel.removeChild("timestamp");
                // and set timestamp as attributes
                setTimestamp(zettel, created, edited);
            }
        }
    }
    /**
     * This method updates the data structure from version 3.3 to version 3.4. With
     * this change, each Zettel-element gets two new child-elements which refer to the
     * previous and next entry. With this references, we can reorder entries without
     * changing their place in the data structure, i.e. the user can re-order entries
     * in their visible order as they appear in the application, while their "id", i.e.
     * their place in the data structure does not change.
     * <br><br>
     * Futhermore, the root-element gets references to the first and the last entry in that order.
     */
    public void db_updateEntryOrderReferences() {
        int first = -1;
        int last = -1;
        // iterate all elements
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            // find first entry in the database that is not deleted
            if (!isDeleted(cnt)) {
                // if we found it, store that entry as first element
                first = cnt;
                break;
            }
        }
        // iterate all elements
        for (int cnt=getCount(ZKNCOUNT); cnt>0; cnt--) {
            // find last entry in the database that is not deleted
            if (!isDeleted(cnt)) {
                // if we found it, store that entry as last element
                last = cnt;
                break;
            }
        }
        // set first and last entry, store
        // them in database
        setFirstZettel(first);
        setLastZettel(last);
//        // iterate all elements
//        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
//            // retrieve element
//            Element zettel = retrieveZettel(cnt);
//            // here we change the entry's luhmann-numbers (trailing entries) and the
//            // entry's manual links with the unique IDs
//            replaceAttributeNrWithID(zettel);
//        }
        // this variable stores the number of the last entry
        // where a reference to a previous entry has been set.
        // needed for later reference, see below
        int lastPrevZettel = first;
        // iterate all elements
        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
            // retrieve element
            Element zettel = retrieveZettel(cnt);
            // check for valid value
            if (zettel!=null) {
                // check whether entry is not deleted
                if (!isDeleted(cnt)) {
                    // check whether entry is first entry
                    if (cnt==first) {
                        // the last's next entry is this entry
                        setNextZettel(last, cnt);
                        // the previous entry is the last entry
                        setPrevZettel(cnt, last);
                    }
                    // check whether entry is last entry
                    if (cnt==last) {
                        // the last's next entry is the first entry
                        setNextZettel(cnt, first);
                        // the first's previous entry is this (last) entry
                        setPrevZettel(first, cnt);
                        // set this entry's previous zettel
                        setPrevZettel(cnt, lastPrevZettel);
                        // set previous entry's next zettel to this zettel
                        setNextZettel(lastPrevZettel, cnt);
                    }
                    // if entry is neither first nor last entry, but
                    // "in between", set reference from previous entry to this entry
                    if (cnt!=first && cnt!=last) {
                        // set the last previous entry number as previous entry
                        // of the current entry
                        setPrevZettel(cnt, lastPrevZettel);
                        // the previous entry poins to this entry
                        setNextZettel(lastPrevZettel, cnt);
                    }
                    // and make the current entry as reference for the last
                    // previous-set entry
                    lastPrevZettel = cnt;
                }
            }
        }
//        // iterate all elements
//        for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
//            // retrieve element
//            Element zettel = retrieveZettel(cnt);
//            // here we change the entry's luhmann-numbers (trailing entries) and the
//            // entry's manual links with the unique IDs
//            replaceAttributeIDWithNr(zettel);
//        }
        // change modified state
        setModified(true);
    }
    /**
     * This method replaces all numeral references to other entries
     * from manual links and trailing entry references with the referenced
     * entries' IDs.
     * <br><br>
     * That means, the trails and manual link child attributes that contain
     * numbers of other entries will be modified, so these attributes contain
     * the entries' IDs instead of their numbers.
     * 
     * @param zettel the entry element where the number references should be replaced with
     * entry IDs
     */
    private void replaceAttributeNrWithID(Element zettel) {
        // create string array for attribute iteration
        String[] attributes = new String[] {ELEMENT_TRAILS, ELEMENT_MANLINKS};
        // iterate array
        for (String attr : attributes) {
            // check whether entry has luhmann-element
            if (zettel.getChild(attr)!=null) {
                // get Luhmann-numbers (trailing-entries)
                String luh = zettel.getChild(attr).getText();
                // check whether entry has trailing-numbers
                if (!luh.isEmpty()) {
                    // split them into an array...
                    String[] luhmann = luh.split(",");
                    // prepare string builder
                    StringBuilder sb = new StringBuilder("");
                    // if we have trailing numbers, go on
                    if (luhmann!=null && luhmann.length>0) {
                        for (String luhmann1 : luhmann) {
                            // retrieve luhmann number
                            try {
                                // retrieve number of luhmann entry
                                int nr = Integer.parseInt(luhmann1);
                                // get unique ID of that entry
                                String val = getZettelID(nr);
                                // replace numeral reference with ID number
                                if (val!=null) {
                                    sb.append(val).append(",");
                                }
                            }catch(NumberFormatException ex) {
                            }
                        }
                        // finallay, remove the last ","
                        if (sb.length()>1) {
                            sb.setLength(sb.length()-1);
                        }
                        // update attribute
                        zettel.getChild(attr).setText(sb.toString());
                    }
                }
            }
        }
    }
    /**
     * This method replaces all ID references to other entries
     * from manual links and trailing entry references with the 
     * entries' number.
     * <br><br>
     * That means, the trails and manual link child attributes that have been converted
     * by the {@link #replaceAttributeNrWithID(org.jdom.Element) replaceAttributeNrWithID} method
     * will be "resetted", so these attributes contain the entries' numbers again instead of their IDs.
     * 
     * @param zettel the entry element where the IDs should be replaced with
     * the entry number
     */
    private void replaceAttributeIDWithNr(Element zettel) {
        // check whether entry is deleted or not
        if (isDeleted(zettel)) {
            return;
        }
        //
        // here we change the entry's luhmann-numbers (trailing entries) and the
        // entry's manual links with the unique IDs
        //
        // create string array for attribute iteration
        String [] attributes = new String[] {ELEMENT_TRAILS, ELEMENT_MANLINKS};
        // iterate array
        for (String attr : attributes) {
            // we have to re-convert the unique entry-ID's to their
            // related entry-numbers, which we do here
            // first, retrieve list of IDs
            String [] values = zettel.getChild(attr).getText().split(",");
            // create variable with re-converted values
            StringBuilder final_values = new StringBuilder("");
            // check for valid values                            
            if (values.length>0) {
                // iterate all numbers, which are at the moment
                // still unique IDs instead of numbers (referring to an entry)
                for (String v : values) {
                    // retrieve entry number of that entry with
                    // the uniqe ID stored in "l"
                    int nr = findZettelFromID(v);
                    // check whether ID was found
                    if (nr!=-1) {
                        // append number to stringbuilder
                        final_values.append(String.valueOf(nr)).append(",");
                    }
                }
                // check whether we have any values in the stringbuilder
                // and truncate last comma
                if (final_values.length()>0) {
                    final_values.setLength(final_values.length()-1);
                }
            }
            // set back converted parameters
            zettel.getChild(attr).setText(final_values.toString());
        }
    }
    /**
     * This method returns the number of the next Zettel which is positioned
     * after the Zettel with the number {@code pos}. This method is needed
     * for reordering the entries without changing their original number,
     * i.e. position in the database.<br><br>
     * This method retrieves the entry-number of the next entry in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_NEXT_ZETTEL} of each {@code zettel} element.
     * 
     * @param pos The number or position of the current entry.
     * @return The number of the next entry in the entry-order, or {@code -1}
     * if no such entry or element exists.
     */
    public int getNextZettel(int pos) {
        // retrieve current zettel-element
        Element zettel = retrieveZettel(pos);
        // check for valid value
        if (zettel!=null) {
            // retrieve attribute
            String next = zettel.getAttributeValue(ATTRIBUTE_NEXT_ZETTEL);
            // check whether we have a valid attribute that refers to the
            // next entry
            if (next!=null && !next.isEmpty()) {
                try {
                    // retrieve number
                    int nr = Integer.parseInt(next);
                    // check whether number is within bounds
                    if (nr<1 || nr >getCount(ZKNCOUNT)) {
                        nr = getFirstZettel();
                    }
                    // return result
                    return nr;
                }
                catch (NumberFormatException ex) {
                }
            }
        }
        return -1;
    }
    /**
     * This method sets the number of the next Zettel which is positioned
     * after the Zettel with the number {@code pos}. This method is needed
     * for reordering the entries without changing their original number,
     * i.e. position in the database.<br><br>
     * Sample:
     * {@code setNextZettel(5,9)} sets the reference to the next entry of
     * entry 5 to number 9, i.e. entry 5 points to entry 9 as next entry.
     * 
     * @param pos The number or position of the current entry, where the reference
     * to the next entry should be set
     * @param nr the number of the next entry, where the entry {@code pos} should
     * refer to.
     */
    public void setNextZettel(int pos, int nr) {
        // retrieve current zettel-element
        Element zettel = retrieveZettel(pos);
        // check for valid value
        if (zettel!=null) {
            // check whether number is within bounds
            if (nr>=1 && nr <=getCount(ZKNCOUNT) || -1==nr /* We need -1 here for restting the value */) {
                // set attribute
                zettel.setAttribute(ATTRIBUTE_NEXT_ZETTEL, String.valueOf(nr));
            }
        }
    }
    /**
     * This method returns the number of the first Zettel in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_FIRST_ZETTEL} of the root element {@link #zknFile}.
     * 
     * @return The number of the first entry in the entry-order, or {@code -1}
     * if no such entry or element exists.
     */
    public int getFirstZettel() {
        // check for valid value
        if (zknFile!=null) {
            // retrieve attribute
            String first = zknFile.getRootElement().getAttributeValue(ATTRIBUTE_FIRST_ZETTEL);
            // check whether we have a valid attribute that refers to the
            // first entry
            // next entry
            if (first!=null && !first.isEmpty()) {
                try {
                    // retrieve number
                    int nr = Integer.parseInt(first);
                    
                    // Seit Version 3.1.7: auskommentiert, da Wert auch -1
                    // sein kann
                    
                    // check whether number is within bounds
                    if (/* nr<1 || */ nr >getCount(ZKNCOUNT)) {
                        nr = 1;
                    }
                    // return result
                    return nr;
                }
                catch (NumberFormatException ex) {
                }
            }
        }
        return -1;
    }
    /**
     * This method checks whether the given entry with the number {@code nr} is the first
     * entry in the entry order.
     * 
     * @param nr the number of an entry that should be checked whether it's the first entry in
     * the order or not.
     * @return {@code true} if the entry {@code nr} is the first entry in the order,
     * {@code false otherwise}
     */
    public boolean isFirstZettel(int nr) {
        return (getFirstZettel()==nr);
    }
    /**
     * This method checks whether the given entry with the number {@code nr} is the last
     * entry in the entry order.
     * 
     * @param nr the number of an entry that should be checked whether it's the lasz entry in
     * the order or not.
     * @return {@code true} if the entry {@code nr} is the last entry in the order,
     * {@code false otherwise}
     */
    public boolean isLastZettel(int nr) {
        return (getLastZettel()==nr);
    }
    /**
     * This method sets the number of the first Zettel in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_FIRST_ZETTEL} of the root element {@link #zknFile}.
     * 
     * @param nr the number of the first entry in the user-defined entry-order
     */
    public void setFirstZettel(int nr) {
        // check for valid value
        if (zknFile!=null) {
            // check whether number is within bounds
            if ((nr>=1 && nr <=getCount(ZKNCOUNT)) || -1==nr /* We need this to reset this value */) {
                // set attribute
                zknFile.getRootElement().setAttribute(ATTRIBUTE_FIRST_ZETTEL, String.valueOf(nr));
            }
        }
    }
    /**
     * This method returns the number of the previous Zettel which is positioned
     * before the Zettel with the number {@code pos}. This method is needed
     * for reordering the entries without changing their original number,
     * i.e. position in the database.<br><br>
     * This method retrieves the entry-number of the previous entry in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_PREV_ZETTEL} of each {@code zettel} element.
     * 
     * @param pos The number or position of the current entry.
     * @return The number of the previous entry in the entry-order, or {@code -1}
     * if no such entry or element exists.
     */
    public int getPrevZettel(int pos) {
        // retrieve current zettel-element
        Element zettel = retrieveZettel(pos);
        // check for valid value
        if (zettel!=null) {
            // retrieve attribute
            String next = zettel.getAttributeValue(ATTRIBUTE_PREV_ZETTEL);
            // check whether we have a valid attribute that refers to the
            // next entry
            if (next!=null && !next.isEmpty()) {
                try {
                    // retrieve number
                    int nr = Integer.parseInt(next);
                    // check whether number is within bounds
                    if (nr<1 || nr >getCount(ZKNCOUNT)) {
                        nr = getLastZettel();
                    }
                    // return result
                    return nr;
                }
                catch (NumberFormatException ex) {
                }
            }
        }
        return -1;
    }
    /**
     * This method sets the number of the previous Zettel which is positioned
     * before the Zettel with the number {@code pos}. This method is needed
     * for reordering the entries without changing their original number,
     * i.e. position in the database.<br><br>
     * Sample:
     * {@code setPrevZettel(5,9)} sets the reference to the previous entry of
     * entry 5 to number 9, i.e. entry 5 points to entry 9 as previous entry.
     * 
     * @param pos The number or position of the current entry, where the reference
     * to the previous entry should be set
     * @param nr the number of the previous entry, where the entry {@code pos} should
     * refer to.
     */
    public void setPrevZettel(int pos, int nr) {
        // retrieve current zettel-element
        Element zettel = retrieveZettel(pos);
        // check for valid value
        if (zettel!=null) {
            // check whether number is within bounds
            if (nr>=1 && nr <=getCount(ZKNCOUNT) || -1==nr /* We need -1 here for restting the value */) {
                // set attribute
                zettel.setAttribute(ATTRIBUTE_PREV_ZETTEL, String.valueOf(nr));
            }
        }
    }
    /**
     * This method returns the number of the last Zettel in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_LAST_ZETTEL} of the root element {@link #zknFile}.
     * 
     * @return The number of the last entry in the entry-order, or {@code -1}
     * if no such entry or element exists.
     */
    public int getLastZettel() {
        // check for valid value
        if (zknFile!=null) {
            // retrieve attribute
            String last = zknFile.getRootElement().getAttributeValue(ATTRIBUTE_LAST_ZETTEL);
            // check whether we have a valid attribute that refers to the
            // first entry
            // next entry
            if (last!=null && !last.isEmpty()) {
                try {
                    // retrieve number
                    int nr = Integer.parseInt(last);
                    // Seit Version 3.1.7: auskommentiert, da Wert auch -1
                    // sein kann
                    // check whether number is within bounds
                    if (/* nr<1 || */ nr >getCount(ZKNCOUNT)) {
                        nr = 1;
                    }
                    // return result
                    return nr;
                }
                catch (NumberFormatException ex) {
                }
            }
        }
        return -1;
    }
    /**
     * This method sets the number of the last Zettel in the
     * user-defined order, which is stored in the attribute
     * {@link #ATTRIBUTE_LAST_ZETTEL} of the root element {@link #zknFile}.
     * 
     * @param nr the number of the last entry in the user-defined entry-order
     */
    public void setLastZettel(int nr) {
        // check for valid value
        if (zknFile!=null) {
            // check whether number is within bounds
            if ((nr>=1 && nr <=getCount(ZKNCOUNT)) || -1==nr /* We need this to reset this value */) {
                // set attribute
                zknFile.getRootElement().setAttribute(ATTRIBUTE_LAST_ZETTEL, String.valueOf(nr));
            }
        }
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param zettel the entry-element in the XML-database.
     * @param created the new value for the creation timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     * @param edited the new value for the last modification timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestamp(Element zettel, String created, String edited) {
        // check for valid parameter
        if (zettel!=null) {
            // check for valid parameter and change created attribute
            if (created!=null) {
                zettel.setAttribute(ATTRIBUTE_TIMESTAMP_CREATED, created);
            }
            // check for valid parameter and change edited attribute
            if (edited!=null) {
                zettel.setAttribute(ATTRIBUTE_TIMESTAMP_EDITED, edited);
            }
            setModified(true);
        }
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param zettel the entry-element in the XML-database.
     * @param edited the new value for the last modification timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestampEdited(Element zettel, String edited) {
        // check for valid parameter
        if (zettel!=null) {
            // check for valid parameter and change edited attribute
            if (edited!=null) {
                zettel.setAttribute(ATTRIBUTE_TIMESTAMP_EDITED, edited);
            }
            setModified(true);
        }
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param zettel the entry-element in the XML-database.
     * @param created the new value for the creation timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestampCreated(Element zettel, String created) {
        // check for valid parameter
        if (zettel!=null) {
            // check for valid parameter and change created attribute
            if (created!=null) {
                zettel.setAttribute(ATTRIBUTE_TIMESTAMP_CREATED, created);
            }
            setModified(true);
        }
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param nr the entry-number of the entry which timestamp should be changed
     * @param edited the new value for the last modification timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestampEdited(int nr, String edited) {
        setTimestampEdited(retrieveZettel(nr), edited);
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param nr the entry-number of the entry which timestamp should be changed
     * @param created the new value for the creation timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestampCreated(int nr, String created) {
        setTimestampCreated(retrieveZettel(nr), created);
    }
    /**
     * This method changes the timestamp attributes of an entry.
     * 
     * @param nr the entry-number of the entry which timestamp should be changed
     * @param created the new value for the creation timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     * @param edited the new value for the last modification timestamp of an entry as string.
     * Use {@code null} if this attribute value should not be changed.
     */
    public void setTimestamp(int nr, String created, String edited) {
        setTimestamp(retrieveZettel(nr), created, edited);
    }
    /**
     * This method creates a new unique ID for an entry and adds it as ID-attribute to
     * the zettel-XML-element.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the entry that shoud receive a new ID.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(ZKNCOUNT)}
     */
    public void setZettelID(int nr) {
        setZettelID(retrieveZettel(nr));
    }
    /**
     * This method creates a new unique ID for an entry and adds it as ID-attribute to
     * the zettel-XML-element.
     * <br><br>
     *
     * @param zettel the entry that shoud receive a new ID, as XML-element
     */
    public void setZettelID(Element zettel) {
        // check for valid value
        if (zettel!=null) {
            // now add id to zettel-element
            zettel.setAttribute(ATTRIBUTE_ZETTEL_ID, Tools.createZknID(settings.getFileName()));
            // change modified state
            setModified(true);
        }
    }
    /**
     * This method retrieves the new unique ID for an entry.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the entry of which ID shoud be retrieved.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(ZKNCOUNT)}
     * @return the unique ID of an entry as string-value, or {@code null} if no ID was found.
     */
    public String getZettelID(int nr) {
        return getZettelID(retrieveZettel(nr));
    }
    /**
     * This method retrieves the new unique ID for an entry.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param zettel the entry-element which ID shoud be retrieved.
     * @return the unique ID of an entry as string-value, or {@code null} if no ID was found.
     */
    public String getZettelID(Element zettel) {
        // check for valid value
        if (zettel!=null) {
            // now add id to zettel-element
            String id = zettel.getAttributeValue(ATTRIBUTE_ZETTEL_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return id;
            }
        }
        return null;
    }
    /**
     * This method stores the ID number of the last new added entry, so we
     * always know which entry was latest added to the data base.
     * 
     * @param zettel the entry-element, which ID should be saved.
     */
    public void setLastAddedZettelID(Element zettel) {
        lastAddedZettelID = getZettelID(zettel);
    }
    /**
     * This method returns the ID of the last new added entry that
     * was added to the data base.
     * 
     * @return the ID of the last new added entry that was added to the data base.
     */
    public String getLastAddedZettelID() {
        return lastAddedZettelID;
    }
    /**
     * This method retrieves the new unique ID for an author-element.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the author-element of which ID shoud be retrieved.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(AUCOUNT)}
     * @return the unique ID of an author-element as string-value, or {@code null} if no ID was found.
     */
    public String getAuthorID(int nr) {
        // retrieve element
        Element author = retrieveElement(authorFile, nr);
        // check for valid value
        if (author!=null) {
            // now add id to zettel-element
            String id = author.getAttributeValue(ATTRIBUTE_AUTHOR_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return id;
            }
        }
        return null;
    }
    /**
     * This method retrieves the unique ID for an author-element.
     *
     * @param au the string value (case sensitive) of the author-element of which ID shoud be retrieved.
     * @return the unique ID of a author-element as string-value, or {@code null} if no ID was found.
     */
    public String getAuthorID(String au) {
        // check for valid value
        if (au!=null && !au.isEmpty()) {
            // find author
            int pos = findAuthorInDatabase(au);
            // check whether author was found or not
            if (pos!=-1) {
                // return author ID
                return getAuthorID(pos);
            }
        }
        return null;
    }
    /**
     * This method retrieves the new unique ID for a keyword-element.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(KWCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the keyword-element of which ID shoud be retrieved.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(KWCOUNT)}
     * @return the unique ID of a keyword-element as string-value, or {@code null} if no ID was found.
     */
    public String getKeywordID(int nr) {
        // retrieve element
        Element keyword = retrieveElement(keywordFile, nr);
        // check for valid value
        if (keyword!=null) {
            // now add id to zettel-element
            String id = keyword.getAttributeValue(ATTRIBUTE_KEYWORD_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return id;
            }
        }
        return null;
    }
    /**
     * This method retrieves the unique ID for a keyword-element.
     *
     * @param kw the string value (case sensitive) of the keyword-element of which ID shoud be retrieved.
     * @return the unique ID of a keyword-element as string-value, or {@code null} if no ID was found.
     */
    public String getKeywordID(String kw) {
        // check for valid value
        if (kw!=null && !kw.isEmpty()) {
            // find keyword
            int pos = findKeywordInDatabase(kw);
            // check whether kw was found or not
            if (pos!=-1) {
                // return keyword ID
                return getKeywordID(pos);
            }
        }
        return null;
    }
    /**
     * This method finds / retrieves the entry-number of an entry with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #getZettelNumberFromID(java.lang.String)}.</i>
     * 
     * @param id the unique entry-ID of an entry, which entry-number should be retrieved
     * @return the entry-number of that entry with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int findZettelFromID(String id) {
        // check for valid parameter
        if (id!=null && !id.isEmpty()) {
            // go through all entries in database
            for (int cnt=1; cnt<=getCount(ZKNCOUNT); cnt++) {
                // retrieve entry's unique ID
                String zettelid = getZettelID(cnt);
                // check for valid ID
                if (zettelid!=null) {
                    // when parameter-ID matches found ID, return entry-number
                    if (id.equals(zettelid)) {
                        return cnt;
                    }
                }
            }
        }
        return -1;
    }
    /**
     * This method finds / retrieves the author-number of an author-element with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #getAuthorNumberFromID(java.lang.String)}.</i>
     * 
     * @param id the unique author-ID of an entry, which author -number should be retrieved
     * @return the author-number of that author-element with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int findAuthorFromID(String id) {
        // check for valid parameter
        if (id!=null && !id.isEmpty()) {
            // go through all entries in database
            for (int cnt=1; cnt<=getCount(AUCOUNT); cnt++) {
                // retrieve author's unique ID
                String authorid = getAuthorID(cnt);
                // check for valid ID
                if (authorid!=null) {
                    // when parameter-ID matches found ID, return entry-number
                    if (id.equals(authorid)) {
                        return cnt;
                    }
                }
            }
        }
        return -1;
    }
    /**
     * This method finds / retrieves the keyword-number of a keyword-element with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #getKeywordNumberFromID(java.lang.String)}.</i>
     * 
     * @param id the unique keyword-ID of an entry, which keyword-number should be retrieved
     * @return the keyword-number of that keyword-element with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int findKeywordFromID(String id) {
        // check for valid parameter
        if (id!=null && !id.isEmpty()) {
            // go through all entries in database
            for (int cnt=1; cnt<=getCount(KWCOUNT); cnt++) {
                // retrieve author's unique ID
                String keywordid = getKeywordID(cnt);
                // check for valid ID
                if (keywordid!=null) {
                    // when parameter-ID matches found ID, return entry-number
                    if (id.equals(keywordid)) {
                        return cnt;
                    }
                }
            }
        }
        return -1;
    }
    /**
     * This method finds / retrieves the entry-number of an entry with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #findZettelFromID(java.lang.String)}.</i>
     * 
     * @param id the unique entry-ID of an entry, which entry-number should be retrieved
     * @return the entry-number of that entry with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int getZettelNumberFromID(String id) {
        return findZettelFromID(id);
    }
    /**
     * This method finds / retrieves the author-number of an author-element with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #findAuthorFromID(java.lang.String)}.</i>
     * 
     * @param id the unique author-ID of an entry, which author -number should be retrieved
     * @return the author-number of that author-element with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int getAuthorNumberFromID(String id) {
        return findAuthorFromID(id);
    }
    /**
     * This method finds / retrieves the keyword-number of a keyword-element with the
     * unique ID {@code id} that is passed as parameter.<br><br>
     * <b>Caution!</b> This method might be very time consuming, if the data base
     * is huge. Consider using this method only within a background task!
     * <br><br>
     * <i>This method is identical to {@link #findKeywordFromID(java.lang.String)}.</i>
     * 
     * @param id the unique keyword-ID of an entry, which keyword-number should be retrieved
     * @return the keyword-number of that keyword-element with the unique ID {@code id}, or {@code -1}
     * if no such entry was found or an invalid parameter was passed.
     */
    public int getKeywordNumberFromID(String id) {
        return findKeywordFromID(id);
    }
    /**
     * This method checks whether an entry with the unique ID {@code id} already
     * exists in the current data base or not.
     * 
     * @param id the unique entry-ID of an entry which should be checked for existence.
     * @return {@code true} when an entry with the unique ID {@code id} already exists in the data file,
     * {@code false} otherwise.
     */
    public boolean zettelExists(String id) {
        return (findZettelFromID(id)!=-1);
    }
    /**
     * This method checks whether an entry with the number {@code nr}
     * exists in the current data base or not.
     * 
     * @param nr the number of an entry which should be checked for existence.
     * @return {@code true} when an entry with the unique ID {@code id} already exists in the data file,
     * {@code false} otherwise.
     */
    public boolean zettelExists(int nr) {
        return (retrieveZettel(nr)!=null);
    }
    /**
     * This method checks whether a keyword with the unique ID {@code id} already
     * exists in the current data base or not.
     * <br><br>
     * In case you want to look for an existing keyword according to identical
     * Strings (not ID!), use {@link #getKeywordPosition(java.lang.String)}.
     * 
     * @param id the unique keyword-ID of a keyword-element which should be checked for existence.
     * @return {@code true} when a keyword with the unique ID {@code id} already exists in the data file,
     * {@code false} otherwise.
     */
    public boolean keywordExists(String id) {
        return (findKeywordFromID(id)!=-1);
    }
    /**
     * This method checks whether an author with the unique ID {@code id} already
     * exists in the current data base or not.
     * <br><br>
     * In case you want to look for an existing author according to identical
     * Strings (not ID!), use {@link #getAuthorPosition(java.lang.String)}.
     * 
     * @param id the unique author-ID of an author-element which should be checked for existence.
     * @return {@code true} when a author with the unique ID {@code id} already exists in the data file,
     * {@code false} otherwise.
     */
    public boolean authorExists(String id) {
        return (findAuthorFromID(id)!=-1);
    }
    /**
     * This method checks whether the entry with the number {@code nr} has a unique
     * ID assigned to it or not.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(ZKNCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the entry that shoud be checked for a valid ID.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(ZKNCOUNT)}
     * @return {@code true} if an ID was found, {@code false} otherwise.
     */
    public boolean hasZettelID(int nr) {
        // retrieve element
        Element zettel = retrieveZettel(nr);
        // check for valid value
        if (zettel!=null) {
            // now add id to zettel-element
            String id = zettel.getAttributeValue(ATTRIBUTE_ZETTEL_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    /**
     * This method checks whether an author-element with the number {@code nr} has a unique
     * ID assigned to it or not.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(AUCouNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the author-element that shoud be checked for a valid ID.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(AUCOUNT)}
     * @return {@code true} if an ID was found, {@code false} otherwise.
     */
    public boolean hasAuthorID(int nr) {
        // retrieve element
        Element author = retrieveElement(authorFile, nr);
        // check for valid value
        if (author!=null) {
            // now add id to author-element
            String id = author.getAttributeValue(ATTRIBUTE_AUTHOR_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    /**
     * This method checks whether a keyword-element with the number {@code nr} has a unique
     * ID assigned to it or not.
     * <br><br>
     * <b>Caution!</b> The position {@code pos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount(KWCOUNT)} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     *
     * @param nr the number of the keyword-element that shoud be checked for a valid ID.
     * the position of the element, ranged from 1 to {@link #getCount(int) getCount(KWCOUNT)}
     * @return {@code true} if an ID was found, {@code false} otherwise.
     */
    public boolean hasKeywordID(int nr) {
        // retrieve element
        Element keyword = retrieveElement(keywordFile, nr);
        // check for valid value
        if (keyword!=null) {
            // now add id to keyword-element
            String id = keyword.getAttributeValue(ATTRIBUTE_KEYWORD_ID);
            // check for valid value
            if (id!=null && !id.isEmpty()) {
                return true;
            }
        }
        return false;
    }    
    /**
     * Use this method to indicate whether saving the data file was ok, or whether an error
     * occured. This method is typically used in the saving-task, see
     * {@link de.danielluedecke.zettelkasten.tasks.SaveFileTask SaveFileTask}.
     *
     * @param val {@code true} when saving the data file was ok, {@code false} if an error occured.
     */
    public void setSaveOk(boolean val) {
        saveOk = val;
    }
    /**
     * Indicates whether saving the data file was ok, or whether an error
     * occured. See {@link de.danielluedecke.zettelkasten.tasks.SaveFileTask SaveFileTask}.
     * @return {@code true} when saving the data file was ok, {@code false} if an error occured.
     */
    public boolean isSaveOk() {
        return saveOk;
    }
    /**
     * This method extracts all occurences of possible form-tags (Laws of Form)
     * from an entry with the index-number {@code nr} and returns them as an 
     * array list of strings.
     * 
     * @param nr the index-number of the entry where the forms should be retrieved
     * @return an array list with all form-tags of that entry as strings, or {@code null}
     * if no form tag was found.
     */
    public ArrayList<String> getZettelForms(int nr) {
        // retrieve entry content
        return Tools.getFormsFromString(getZettelContent(nr));
    }
    /**
     * This methods check whether formtags within an entry already exist as image files,
     * and if not, it does create these image files.
     * 
     * @param content the content of an entry where the form-tags will be extracted
     * and the form-images will be created.
     */
    private void createFormImagesFromContent(String content) {
        // retrieve form-tags from content
        ArrayList<String> dummy = Tools.getFormsFromString(content);
        // copy them to a string array
        String[] formtags = dummy.toArray(new String[dummy.size()]);
        // iterate all form-tags
        for (String formimg : formtags) {
            // create new instance for creating form images
            CMakeFormImage newFormImage = new CMakeFormImage(this, settings, formimg);
            // create form image
            newFormImage.createFormImage();
            // check for errors
            if (!newFormImage.isSaveImgOk()) {
                zknframe.showErrorIcon();
            }
        }
    }
    /**
     * This method moves an entry in the displayed entry order. The entry keeps its number
     * and will not be moved within the XML data file. only the pointer referenced (see
     * {@link #ATTRIBUTE_NEXT_ZETTEL} and {@link #ATTRIBUTE_PREV_ZETTEL}) are changed.
     * 
     * @param entryToMove The number of that entry that should be moved to another position (source).
     * @param insertAfter The number of that entry after which the {@code entryToMove} should
     * be inserted (destination).
     */
    public void moveEntry(int entryToMove, int insertAfter) {
        // check if equal
        if (entryToMove==insertAfter) {
            return;
        }
        // check whether insertAfter is a valid destination
        if (isDeleted(insertAfter) || !zettelExists(insertAfter)) {
            return;
        }
        // first, check whether the entry is inserted after that entry
        // where it already is sorted in. if yes, don't move anything
        if (getPrevZettel(entryToMove)==insertAfter) {
            return;
        }
        // now, connect those entries which originally refered
        // to the move entry
        // therefor we need the refrenced to the previous
        // and next entry of the move entry
        int moveprev = getPrevZettel(entryToMove);
        int movenext = getNextZettel(entryToMove);
        // now connect these entries, so the move entry
        // is "cut out" of the entry order
        setNextZettel(moveprev, movenext);
        setPrevZettel(movenext, moveprev);
        // check, whether moved entry was the first entry
        // if so, the entry following the move entry is now the new first entry
        if (isFirstZettel(entryToMove)) {
            setFirstZettel(movenext);
        }
        // check, whether moved entry was the last entry
        // if so, the entry before the move entry is now the new last entry
        if (isLastZettel(entryToMove)) {
            setLastZettel(moveprev);
        }
        // now, insert entry at new position
        // therefore we need to "disconnect" those entries where the move entry should be inserted
        // therefor we need the refrence to the next entry of the insertAfter entry
        int nextBehindEntryToMove = getNextZettel(insertAfter);
        setPrevZettel(nextBehindEntryToMove, entryToMove);
        setNextZettel(insertAfter, entryToMove);
        // and insert  the new entry
        setPrevZettel(entryToMove, insertAfter);
        setNextZettel(entryToMove, nextBehindEntryToMove);
        // check whether insertAfter was the last entry in the order
        if (isLastZettel(insertAfter)) {
            setLastZettel(entryToMove);
        }
        // set modified flag
        setModified(true);
        // title list is out of date
        setTitlelistUpToDate(false);
    }
    /**
     * This method moves several entries in their displayed entry order. The entries keeps their number
     * and will not be moved within the XML data file. only the pointer referenced (see
     * {@link #ATTRIBUTE_NEXT_ZETTEL} and {@link #ATTRIBUTE_PREV_ZETTEL}) are changed.
     * 
     * @param entriesToMove An array with numbers of those entries that should be moved to another position (source).
     * @param insertAfter The number of that entry after which the {@code entriesToMove} should
     * be inserted (destination).
     */
    public void moveEntries(int[] entriesToMove, int insertAfter) {
        // check for valid values
        if (entriesToMove!=null && entriesToMove.length>0) {
            // go through the array, but from last to first element.
            // since each entry is inserted after "insertAfter", we keep
            // the order of the moved entries by inserting them from last
            // to first entry
            for (int cnt=entriesToMove.length-1; cnt>=0; cnt--) {
                // move each entry
                moveEntry(entriesToMove[cnt], insertAfter);
            }
        }
    }
    /**
     * This method moves several entries in their displayed entry order. The entries keeps their number
     * and will not be moved within the XML data file. only the pointer referenced (see
     * {@link #ATTRIBUTE_NEXT_ZETTEL} and {@link #ATTRIBUTE_PREV_ZETTEL}) are changed.
     * 
     * @param entriesToMove An array with numbers in String format of those entries that should be moved to another position (source).
     * @param insertAfter The number of that entry after which the {@code entriesToMove} should
     * be inserted (destination).
     */
    public void moveEntries(String[] entriesToMove, int insertAfter) {
        // check for valid values
        if (entriesToMove!=null && entriesToMove.length>0) {
            // go through the array, but from last to first element.
            // since each entry is inserted after "insertAfter", we keep
            // the order of the moved entries by inserting them from last
            // to first entry
            for (int cnt=entriesToMove.length-1; cnt>=0; cnt--) {
                // move each entry
                try {
                    moveEntry(Integer.parseInt(entriesToMove[cnt]), insertAfter);
                }
                catch (NumberFormatException ex) {
                }
            }
        }
    }
    
    
    /**
     * This method tries to find a parent-level follower of the entry 
     * with the number {@code nr}.
     * <br/><br/>
     * <b>Caution!</b> This method might be time consuming. Consider using it only
     * in a background thread.
     * 
     * <br/><br/><b>Caution!</b> The position {@code nr} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param nr the index number of the entry
     * @return the number of the "luhmann"-parent of the entry {@code nr}, or {@code -1} if
     * the entry {@code nr} has no luhmann (follower) parent
     */
    public int findParentlLuhmann(int nr) {
        // init find value
        boolean found = true;
        //
        int retval = -1;
        // 
        while (found) {
            // indicates, whether any luhmann parent was found
            boolean innerLoopFound = false;
            // counter for looping through entries
            int cnt = 1;
            // get current entry number as string
            String currentEntry = String.valueOf(nr);
            // go through complete data set
            while(!innerLoopFound && cnt<=getCount(Daten.ZKNCOUNT)) {
                // get the luhmann-numbers of each entry
                String[] lnrs = getLuhmannNumbers(cnt).split(",");
                // now check each number for the occurence of the current entry number
                for (String l : lnrs) {
                    // when one of the luhmann-numbers equals the current entry number...
                    if (l.equals(currentEntry)) {
                        // we found a parent
                        nr = retval = cnt;
                        // indicate that parent was found
                        innerLoopFound = true;
                        break;
                    }
                }
                // inceare loop counter
                cnt++;
            }
            // when all entries have been checked and no parent was found
            // leave complete routine and return result.
            if (!innerLoopFound) {
                found = false;
            }
        }
        return retval;
    }
    /**
     * This method retrieves all follower and follower's follower of the entry
     * {@code zettelpos} and stores the index numbers in the global integer array
     * {@link #allLuhmannNumbers allLuhmannNumbers}
     * <br/><b>Caution!</b> The position {@code zettelpos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param zettelpos the index number of the entry
     */
    private void retrieveAllLuhmannNumbers(int zettelpos) {
        // get the text from the luhmann-numbers
        String lnr = getLuhmannNumbers(zettelpos);
        // if we have any luhmann-numbers, go on...
        if (!lnr.isEmpty()) {
            // copy all values to an array
            String[] lnrs = lnr.split(",");
            // go throughh array of current luhmann-numbers
            for (String exist : lnrs) {
                // append value to result array
                try {
                    // therefor, convert it to int
                    int nr = Integer.parseInt(exist);
                    // add value to results
                    if (!allLuhmannNumbers.contains(nr)) {
                        allLuhmannNumbers.add(nr);
                    }
                    // check whether more deeper luhmann-value exists, by re-calling this method
                    // again and go through a recusrive loop
                    retrieveAllLuhmannNumbers(nr);
                }
                catch (NumberFormatException ex) {
                }
            }
        }
    }
    /**
     * This method retrieves all follower and follower's follower of the entry
     * {@code zettelpos} and returns the index numbers as integer array.
     * <br/><b>Caution!</b> The position {@code zettelpos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param zettelpos the index number of the entry
     * @return all follower and follower's follower of the entry
     * {@code zettelpos} as integer array
     */
    public int[] getAllLuhmannNumbers(int zettelpos) {
        // reset result array
        allLuhmannNumbers.clear();
        // call method to retrieve all luhmann numbers
        retrieveAllLuhmannNumbers(zettelpos);
        // create int arrey
        int[] arr = new int[allLuhmannNumbers.size()];
        // copy all values into array
        for (int cnt=0; cnt<allLuhmannNumbers.size(); cnt++) {
            arr[cnt] = allLuhmannNumbers.get(cnt);
        }
        // return result
        return arr;
    }
    /**
     * This method checks whether the entry {@code zettelpos} has follower / trails / luhmann
     * numbers or not.
     * <br/><b>Caution!</b> The position {@code zettelpos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param zettelpos the index number of the entry
     * @return {@code true} if the entry {@code zettelpos} has follower / trails / luhmann
     * numbers, {@code false} if not.
     */
    public boolean hasLuhmannNumbers(int zettelpos) {
       // retrieve luhmann numbers
        String lnr = getLuhmannNumbers(zettelpos);
        return (lnr!=null && !lnr.isEmpty());
    }
    /**
     * This method checks whether the entry {@code zettelpos} has manual links / references
     * to other entries or not.
     * <br/><b>Caution!</b> The position {@code zettelpos} is a value from <b>1</b> to
     * {@link #getCount(int) getCount()} - in contrary
     * to usual array handling where the range is from 0 to (size-1).
     * 
     * @param zettelpos the index number of the entry
     * @return {@code true} if the entry {@code zettelpos} has manual links / references to other
     * entries, {@code false} if not.
     */
    public boolean hasManLinks(int zettelpos) {
       // retrieve manual links
        int[] ml = getManualLinks(zettelpos);
        return (ml!=null && ml.length>0);
    }
    /**
     * This method extracts manual links from an entry's content that have been added via
     * the NewEntryFrame.<br><br>
     * All manual link tags {@code [z #number]text[/z]} will be scanned and the numbers
     * (references to other entries) are extracted. All manual links are returned as integer list.
     * 
     * @param dummy the content from an entry as String
     * @return all manual links inside the manual-link-tag {@code [z]} as integer list
     */
    private List<Integer> extractManualLinksFromContent(String dummy) {
        // save manual links
        List<Integer> manlinknumbers = new ArrayList<Integer>();
        try {
            // create foot note patterm
            Pattern p = Pattern.compile("\\[z ([^\\[]*)\\](.*?)\\[/z\\]");
            // create matcher
            Matcher m = p.matcher(dummy);
            // check for occurences
            while (m.find()) {
                // if we found something, we have two groups
                // the 2nd groups contains the manlink number as string
                int ml = Integer.parseInt(dummy.substring(m.start(m.groupCount()-1), m.end(m.groupCount()-1)));
                // add to result list
                manlinknumbers.add(ml);
            }
        }
        catch (PatternSyntaxException ex) {
        }
        catch (IndexOutOfBoundsException ex) {
        }
        catch (NumberFormatException ex) {
        }
        return manlinknumbers;
    }
    /**
     * Creates a sorted String with comma separated values of manual links. Manual links are stored
     * in this String format in the XML database.
     * 
     * @param manlinknumbers (extracted) manual links as integer list. Use {@link #extractManualLinksFromContent(java.lang.String)}
     * to retrieve manual links from an entry's content as integer list
     * @return a String containing all manual links, so this String can be stored as child-element in the
     * XML database (see {@link #ELEMENT_MANLINKS}).
     */
    private String retrievePreparedManualLinksFromContent(List<Integer> manlinknumbers) {
        // add them, if we have any
        if (manlinknumbers!=null && !manlinknumbers.isEmpty()) {
            // convert to array
            Integer[] i = manlinknumbers.toArray(new Integer[manlinknumbers.size()]);
            // sort array
            Arrays.sort(i);
            StringBuilder sb = new StringBuilder("");
            // and add it
            for (int ml : i) {
                sb.append(String.valueOf(ml)).append(",");
            }
            // remove last comma
            if (sb.length()>1) sb.setLength(sb.length()-1);
            // add to element
            return (sb.toString());
        }
        else {
            return("");
        }
    }
}