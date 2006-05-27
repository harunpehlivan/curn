/*---------------------------------------------------------------------------*\
  $Id$
  ---------------------------------------------------------------------------
  This software is released under a Berkeley-style license:

  Copyright (c) 2004-2006 Brian M. Clapper. All rights reserved.

  Redistribution and use in source and binary forms are permitted provided
  that: (1) source distributions retain this entire copyright notice and
  comment; and (2) modifications made to the software are prominently
  mentioned, and a copy of the original software (or a pointer to its
  location) are included. The name of the author may not be used to endorse
  or promote products derived from this software without specific prior
  written permission.

  THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR IMPLIED
  WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.

  Effectively, this means you can do what you want with the software except
  remove this notice or take advantage of the author's name. If you modify
  the software and redistribute your modified version, you must indicate that
  your version is a modification of the original, and you must provide either
  a pointer to or a copy of the original.
\*---------------------------------------------------------------------------*/

package org.clapper.curn;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.net.URL;
import java.net.MalformedURLException;

import org.clapper.util.config.Configuration;
import org.clapper.util.config.ConfigurationException;
import org.clapper.util.config.NoSuchSectionException;
import org.clapper.util.config.NoSuchVariableException;

import org.clapper.util.logging.Logger;

/**
 * <p><tt>CurnConfig</tt> uses the <tt>Configuration</tt> class (part of
 * the <i>clapper.org</i> Java Utility library) to parse and validate the
 * <i>curn</i> configuration file, holding the results in memory for easy
 * access.</p>
 *
 * @version <tt>$Revision$</tt>
 */
public class CurnConfig extends Configuration
{
    /*----------------------------------------------------------------------*\
                             Public Constants
    \*----------------------------------------------------------------------*/

    /**
     * Variable names
     */
    public static final String VAR_CACHE_FILE        = "CacheFile";
    public static final String VAR_TOTAL_CACHE_BACKUPS = "TotalCacheBackups";
    public static final String VAR_NO_CACHE_UPDATE   = "NoCacheUpdate";
    public static final String VAR_SUMMARY_ONLY      = "SummaryOnly";
    public static final String VAR_MAX_SUMMARY_SIZE  = "MaxSummarySize";
    public static final String VAR_SMTPHOST          = "SMTPHost";
    public static final String VAR_MAIL_SUBJECT      = "Subject";
    public static final String VAR_DAYS_TO_CACHE     = "DaysToCache";
    public static final String VAR_PARSER_CLASS_NAME = "ParserClass";
    public static final String VAR_PRUNE_URLS        = "PruneURLs";
    public static final String VAR_SHOW_RSS_VERSION  = "ShowRSSVersion";
    public static final String VAR_SMTP_HOST         = "SMTPHost";
    public static final String VAR_EMAIL_SENDER      = "MailFrom";
    public static final String VAR_EMAIL_SUBJECT     = "MailSubject";
    public static final String VAR_SHOW_DATES        = "ShowDates";
    public static final String VAR_TITLE_OVERRIDE    = "TitleOverride";
    public static final String VAR_EDIT_ITEM_URL     = "EditItemURL";
    public static final String VAR_PREPARSE_EDIT     = "PreparseEdit";
    public static final String VAR_DISABLED          = "Disabled";
    public static final String VAR_SHOW_AUTHORS      = "ShowAuthors";
    public static final String VAR_FEED_URL          = "URL";
    public static final String VAR_CLASS             = "Class";
    public static final String VAR_GET_GZIPPED_FEEDS = "GetGzippedFeeds";
    public static final String VAR_MAX_THREADS       = "MaxThreads";
    public static final String VAR_IGNORE_DUP_TITLES = "IgnoreDuplicateTitles";
    public static final String VAR_FORCE_ENCODING    = "ForceEncoding";
    public static final String VAR_FORCE_CHAR_ENCODING = "ForceCharacterEncoding";
    public static final String VAR_USER_AGENT        = "UserAgent";
    public static final String VAR_ALLOW_EMBEDDED_HTML= "AllowEmbeddedHTML";

    /**
     * Default values
     */
    public static final int     DEF_DAYS_TO_CACHE     = 365;
    public static final boolean DEF_PRUNE_URLS        = false;
    public static final boolean DEF_NO_CACHE_UPDATE   = false;
    public static final boolean DEF_SUMMARY_ONLY      = false;
    public static final int     DEF_MAX_SUMMARY_SIZE  = Integer.MAX_VALUE;
    public static final boolean DEF_SHOW_RSS_VERSION  = false;
    public static final boolean DEF_SHOW_DATES        = false;
    public static final boolean DEF_SHOW_AUTHORS      = false;
    public static final boolean DEF_GET_GZIPPED_FEEDS = true;
    public static final boolean DEF_SAVE_ONLY         = false;
    public static final String  DEF_SMTP_HOST         = "localhost";
    public static final String  DEF_EMAIL_SUBJECT     = "curn output";
    public static final String  DEF_PARSER_CLASS_NAME =
                             "org.clapper.curn.parser.minirss.MiniRSSParser";
    public static final int     DEF_MAX_THREADS       = 5;
    public static final boolean DEF_ALLOW_EMBEDDED_HTML= false;
    public static final int     DEF_TOTAL_CACHE_BACKUPS = 0;

    /**
     * Others
     */
    public static final String  NO_LIMIT_VALUE         = "NoLimit";

    /*----------------------------------------------------------------------*\
                             Private Constants
    \*----------------------------------------------------------------------*/

    /**
     * Main section name
     */
    private static final String  MAIN_SECTION         = "curn";

    /**
     * Prefix for sections that describing individual feeds.
     */
    private static final String FEED_SECTION_PREFIX   = "Feed";

    /**
     * Prefix for output handler sections.
     */
    private static final String OUTPUT_HANDLER_PREFIX = "OutputHandler";

    /*----------------------------------------------------------------------*\
                            Private Data Items
    \*----------------------------------------------------------------------*/

    private File cacheFile = null;
    private int defaultCacheDays = DEF_DAYS_TO_CACHE;
    private boolean updateCache = true;
    private boolean summaryOnly = false;
    private boolean showRSSFormat = false;
    private boolean showDates = false;
    private Collection<FeedInfo> feeds = new ArrayList<FeedInfo>();
    private Map<String, FeedInfo> feedMap = new HashMap<String, FeedInfo>();
    private String parserClassName = DEF_PARSER_CLASS_NAME;
    private List<ConfiguredOutputHandler> outputHandlers
                                 = new ArrayList<ConfiguredOutputHandler>();
    private String smtpHost = DEF_SMTP_HOST;
    private String emailSender = null;
    private String emailSubject = DEF_EMAIL_SUBJECT;
    private boolean showAuthors = false;
    private boolean getGzippedFeeds = true;
    private int maxThreads = DEF_MAX_THREADS;
    private String defaultUserAgent = null;
    private int maxSummarySize = DEF_MAX_SUMMARY_SIZE;
    private boolean allowEmbeddedHTML = DEF_ALLOW_EMBEDDED_HTML;
    private int totalCacheBackups = DEF_TOTAL_CACHE_BACKUPS;

    /**
     * For log messages
     */
    private static Logger log = new Logger (CurnConfig.class);

    /*----------------------------------------------------------------------*\
                                Constructor
    \*----------------------------------------------------------------------*/

    /**
     * Construct an <tt>CurnConfig</tt> object that parses data
     * from the specified file.
     *
     * @param f  The <tt>File</tt> to open and parse
     *
     * @throws IOException             can't open or read file
     * @throws ConfigurationException  error in configuration data
     * @throws CurnException           some other error
     */
    CurnConfig (File f)
        throws IOException,
               ConfigurationException,
               CurnException
    {
        super (f);
        validate();
    }

    /**
     * Construct an <tt>CurnConfig</tt> object that parses data
     * from the specified file.
     *
     * @param path  the path to the file to parse
     *
     * @throws FileNotFoundException   specified file doesn't exist
     * @throws IOException             can't open or read file
     * @throws ConfigurationException  error in configuration data
     * @throws CurnException           some other error
     */
    CurnConfig (String path)
        throws FileNotFoundException,
               IOException,
               ConfigurationException,
               CurnException
    {
        super (path);
        validate();
    }

    /**
     * Construct an <tt>CurnConfig</tt> object that parses data
     * from the specified URL.
     *
     * @param url  the URL to open and parse
     *
     * @throws IOException             can't open or read URL
     * @throws ConfigurationException  error in configuration data
     * @throws CurnException           some other error
     */
    CurnConfig (URL url)
        throws IOException,
               ConfigurationException,
               CurnException
    {
        super (url);
        validate();
    }

    /**
     * Construct an <tt>CurnConfig</tt> object that parses data
     * from the specified <tt>InputStream</tt>.
     *
     * @param iStream  the <tt>InputStream</tt>
     *
     * @throws IOException             can't open or read URL
     * @throws ConfigurationException  error in configuration data
     * @throws CurnException           some other error
     */
    CurnConfig (InputStream iStream)
        throws IOException,
               ConfigurationException,
               CurnException
    {
        super (iStream);
        validate();
    }

    /*----------------------------------------------------------------------*\
                              Public Methods
    \*----------------------------------------------------------------------*/

    /**
     * Get the name of the RSS parser class to use. The caller is responsible
     * for loading the returned class name and verifying that it implements
     * the appropriate interface(s).
     *
     * @return the full class name
     */
    public String getRSSParserClassName()
    {
        return parserClassName;
    }

    /**
     * Gets the list of output handlers from the configuration, in the order
     * they appeared in the configuration.
     *
     * @return an unmodifiable <tt>Collection</tt> of
     *         <tt>ConfiguredOutputHandler</tt> objects. The collection will
     *         be empty, but never null, if no output handlers were configured.
     */
    public Collection<ConfiguredOutputHandler> getOutputHandlers()
    {
        return Collections.unmodifiableList (outputHandlers);
    }

    /**
     * Determine whether the configuration is a "download-only" configuration
     * (i.e., one that downloads various RSS feed data but doesn't parse it.).
     *
     * @return <tt>true</tt> if this is a download-only configuration,
     *         <tt>false</tt> otherwise
     */
    public boolean isDownloadOnly()
    {
        return outputHandlers.size() == 0;
    }

    /**
     * Return the total number of configured output handlers.
     *
     * @return the total number of configured output handlers, or 0 if there
     *         aren't any
     */
    public int totalOutputHandlers()
    {
        return outputHandlers.size();
    }

    /**
     * Get the configured cache file.
     *
     * @return the cache file
     *
     * @see #mustUpdateCache
     * @see #setMustUpdateCacheFlag
     */
    public File getCacheFile()
    {
        return cacheFile;
    }

    /**
     * Get the total number of cache backup files to keep.
     *
     * @return the total number of cache backup files to keep, or 0 for
     *         none.
     */
    public int totalCacheBackups()
    {
        return totalCacheBackups;
    }

    /**
     * Determine whether the cache should be updated.
     *
     * @return <tt>true</tt> if the cache should be updated, <tt>false</tt>
     *         if it should not.
     *
     * @see #getCacheFile
     * @see #setMustUpdateCacheFlag
     */
    public boolean mustUpdateCache()
    {
        return updateCache;
    }

    /**
     * Get the maximum number of concurrent threads to spawn when retrieving
     * RSS feeds.
     *
     * @return the maximum number of threads
     *
     * @see #setMaxThreads
     */
    public int getMaxThreads()
    {
        return maxThreads;
    }

    /**
     * Set the maximum number of concurrent threads to spawn when retrieving
     * RSS feeds.
     *
     * @param newValue the maximum number of threads
     *
     * @throws ConfigurationException bad value
     *
     * @see #getMaxThreads
     */
    public void setMaxThreads (int newValue)
        throws ConfigurationException
    {
        if (newValue <= 0)
        {
            throw new ConfigurationException (Constants.BUNDLE_NAME,
                                              "CurnConfig.badPositiveInteger",
                                              "The \"{0}\" configuration "
                                            + "parameter cannot be set to "
                                            + "{1}. It must have a positive "
                                            + "integer value.",
                                              new Object[]
                                              {
                                                  VAR_MAX_THREADS,
                                                  String.valueOf (newValue)
                                              });
        }

        this.maxThreads = newValue;
    }

    /**
     * Get the maximum summary size.
     *
     * @return the maximum summary size, in characters, or 0 for no limit.
     *
     * @see #setMaxSummarySize
     */
    public int getMaxSummarySize()
    {
        return maxSummarySize;
    }

    /**
     * Set the maximum summary size.
     *
     * @param newSize the new maximum summary size, in characters, or 0 for
     *                no limit. Value must be non-negative.
     *
     * @see #getMaxSummarySize
     */
    public void setMaxSummarySize (int newSize)
    {
        assert (newSize >= 0);
        this.maxSummarySize = newSize;
    }

    /**
     * Change the "update cache" flag.
     *
     * @param val <tt>true</tt> if the cache should be updated, <tt>false</tt>
     *            if it should not
     *
     * @see #mustUpdateCache
     * @see #getCacheFile
     */
    public void setMustUpdateCacheFlag (boolean val)
    {
        updateCache = val;
    }

    /**
     * Determine whether to retrieve RSS feeds via Gzip. Only applicable
     * when connecting to HTTP servers.
     *
     * @return <tt>true</tt> if Gzip is to be used, <tt>false</tt>
     *         otherwise
     *
     * @see #setRetrieveFeedsWithGzipFlag
     */
    public boolean retrieveFeedsWithGzip()
    {
        return getGzippedFeeds;
    }

    /**
     * Set the flag that controls whether to retrieve RSS feeds via Gzip.
     * Only applicable when connecting to HTTP servers.
     *
     * @param val <tt>true</tt> if Gzip is to be used, <tt>false</tt>
     *            otherwise
     *
     * @see #retrieveFeedsWithGzip
     */
    public void setRetrieveFeedsWithGzipFlag (boolean val)
    {
        this.getGzippedFeeds = val;
    }

    /**
     * Return the value of the global "allow embedded HTML" flag. This flag
     * controls whether or not embedded HTML markup should be honored or
     * suppressed by default. It can be overridden on a per-feed basis.
     * (Even if set, this flag may not be meaningful to all output
     * handlers.)
     *
     * @return <tt>true</tt> if, by default, embedded HTML markup should be
     *         honored (if possible), <tt>false</tt> if it should
     *         be stripped.
     *
     * @see #setAllowEmbeddedHTMLFlag
     */
    public boolean allowEmbeddedHTML()
    {
        return allowEmbeddedHTML;
    }

    /**
     * Set the global "allow embedded HTML" flag. This flag controls
     * whether or not embedded HTML markup should be honored or suppressed
     * by default. It can be overridden on a per-feed basis. (Even if set,
     * this flag may not be meaningful to all output handlers.)
     *
     * @param allow <tt>true</tt> if, by default, embedded HTML markup should
     *              be honored (if possible), <tt>false</tt> if it should
     *              be stripped.
     *
     * @see #allowEmbeddedHTML()
     */
    public void setAllowEmbeddedHTMLFlag (boolean allow)
    {
        this.allowEmbeddedHTML = allow;
    }

    /**
     * Return the value of the global "show authors" flag. This flag
     * controls whether to display the authors associated with each item,
     * if available. It can be overridden on a per-feed basis.
     *
     * @return <tt>true</tt> if "show authors" flag is set, <tt>false</tt>
     *         otherwise
     *
     * @see #setShowAuthorsFlag
     */
    public boolean showAuthors()
    {
        return showAuthors;
    }

    /**
     * Set the value of the global "show authors" flag. This flag controls
     * whether to display the authors associated with each item, if
     * available. It can be overridden on a per-feed basis.
     *
     * @param val <tt>true</tt> to set the "show authors" flag, <tt>false</tt>
     *            to clear it
     *
     * @see #showAuthors
     */
    public void setShowAuthorsFlag (boolean val)
    {
        this.showAuthors = val;
    }

    /**
     * Return the value of the "show dates" flag. This flag controls whether
     * to display the dates associated with each feed and item, if available.
     *
     * @return <tt>true</tt> if "show dates" flag is set, <tt>false</tt>
     *         otherwise
     *
     * @see #setShowDatesFlag
     */
    public boolean showDates()
    {
        return showDates;
    }

    /**
     * Set the value of the "show dates" flag. This flag controls whether
     * to display the dates associated with each feed and item, if available.
     *
     * @param val <tt>true</tt> to set the "show dates" flag, <tt>false</tt>
     *            to clear it
     *
     * @see #showDates
     */
    public void setShowDatesFlag (boolean val)
    {
        this.showDates = val;
    }

    /**
     * Return the value of "show RSS version" flag.
     *
     * @return <tt>true</tt> if flag is set, <tt>false</tt> if it isn't
     *
     * @see #setShowRSSVersionFlag
     */
    public boolean showRSSVersion()
    {
        return showRSSFormat;
    }

    /**
     * Set the value of the "show RSS version" flag.
     *
     * @param val <tt>true</tt> to set the flag,
     *            <tt>false</tt> to clear it
     *
     * @see #showRSSVersion
     */
    public void setShowRSSVersionFlag (boolean val)
    {
        this.showRSSFormat = val;
    }

    /**
     * Get the SMTP host to use when sending email.
     *
     * @return the SMTP host. Never null.
     *
     * @see #setSMTPHost
     */
    public String getSMTPHost()
    {
        return smtpHost;
    }

    /**
     * Set the SMTP host to use when sending email.
     *
     * @param host the SMTP host, or null to revert to the default value
     *
     * @see #getSMTPHost
     */
    public void setSMTPHost (String host)
    {
        smtpHost = (host == null) ? DEF_SMTP_HOST : host;
    }

    /**
     * Get the email address to use as the sender for email messages.
     *
     * @return the email address, or null if not specified
     *
     * @see #setEmailSender
     */
    public String getEmailSender()
    {
        return emailSender;
    }

    /**
     * Set the email address to use as the sender for email messages.
     *
     * @param address the new address, or null to clear the field
     *
     * @see #getEmailSender
     */
    public void setEmailSender (String address)
    {
        this.emailSender = address;
    }

    /**
     * Get the subject to use in email messages, if email is being sent.
     *
     * @return the subject. Never null.
     *
     * @see #setEmailSubject
     */
    public String getEmailSubject()
    {
        return emailSubject;
    }

    /**
     * Set the subject to use in email messages, if email is being sent.
     *
     * @param subject the subject, or null to reset to the default
     *
     * @see #getEmailSubject
     */
    public void setEmailSubject (String subject)
    {
        this.emailSubject = (subject == null) ? DEF_EMAIL_SUBJECT : subject;
    }

    /**
     * Get the configured RSS feeds.
     *
     * @return a <tt>Collection</tt> of <tt>FeedInfo</tt> objects.
     *
     * @see #hasFeed
     * @see #getFeedInfoFor(String)
     * @see #getFeedInfoFor(URL)
     */
    public Collection<FeedInfo> getFeeds()
    {
        return Collections.unmodifiableCollection (feeds);
    }

    /**
     * Determine whether the specified URL is one of the configured RSS
     * feeds.
     *
     * @param url  the URL
     *
     * @return <tt>true</tt> if it's there, <tt>false</tt> if not
     *
     * @see #getFeeds
     * @see #getFeedInfoFor(String)
     * @see #getFeedInfoFor(URL)
     */
    public boolean hasFeed (URL url)
    {
        return feedMap.containsKey (url.toString());
    }

    /**
     * Get the feed data for a given URL.
     *
     * @param url   the URL
     *
     * @return the corresponding <tt>RSSFeedInfo</tt> object, or null
     *         if not found
     *
     * @see #getFeeds
     * @see #hasFeed
     * @see #getFeedInfoFor(String)
     * @see FeedInfo
     */
    public FeedInfo getFeedInfoFor (URL url)
    {
        return (FeedInfo) feedMap.get (url.toString());
    }

    /**
     * Get the feed data for a given URL.
     *
     * @param urlString   the URL, as a string
     *
     * @return the corresponding <tt>FeedInfo</tt> object, or null
     *         if not found
     *
     * @see #getFeeds
     * @see #hasFeed
     * @see #getFeedInfoFor(URL)
     * @see FeedInfo
     */
    public FeedInfo getFeedInfoFor (String urlString)
    {
        return (FeedInfo) feedMap.get (urlString);
    }

    /*----------------------------------------------------------------------*\
                              Private Methods
    \*----------------------------------------------------------------------*/

    /**
     * Validate the loaded configuration.
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void validate()
        throws ConfigurationException,
               CurnException
    {
        // First, verify that the main section is there and process it.

        processMainSection();

        // Process the remaining sections. Skip ones we don't recognize.

        for (String sectionName : getSectionNames())
        {
            if (sectionName.startsWith (FEED_SECTION_PREFIX))
                processFeedSection (sectionName);

            else if (sectionName.startsWith (OUTPUT_HANDLER_PREFIX))
                processOutputHandlerSection (sectionName);

            else
                processUnknownSection (sectionName);
        }
    }

    /**
     * Verify existence of main section and process it.
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void processMainSection()
        throws ConfigurationException,
               CurnException
    {
        if (! this.containsSection (MAIN_SECTION))
        {
            throw new ConfigurationException (Constants.BUNDLE_NAME,
                                              "CurnConfig.missingReqSection",
                                              "The configuration file is "
                                            + "missing the required \"{0}\" "
                                            + "section.",
                                              new Object[] {MAIN_SECTION});
        }

        for (String varName : getVariableNames (MAIN_SECTION))
        {
            try
            {
                processMainSectionVariable (varName);
            }

            catch (NoSuchVariableException ex)
            {
                throw new ConfigurationException (Constants.BUNDLE_NAME,
                                                  "CurnConfig.missingReqVar",
                                                  "The configuration file is "
                                                + "missing required variable "
                                                + "\"{0}\" in section "
                                                + "\"{1}\".",
                                                  new Object[]
                                                  {
                                                      ex.getVariableName(),
                                                      ex.getSectionName()
                                                  });
            }
        }
    }

    /**
     * Process a single variable from the main section
     *
     * @param varName the variable name
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void processMainSectionVariable (String varName)
        throws ConfigurationException,
               CurnException
    {
        String val = null;

        if (varName.equals (VAR_CACHE_FILE))
        {
            val = getOptionalStringValue (MAIN_SECTION, VAR_CACHE_FILE, null);
            if (val != null)
            {
                cacheFile = new File (val);
                if (cacheFile.isDirectory())
                {
                    throw new ConfigurationException
                        (Constants.BUNDLE_NAME,
                         "CurnConfig.cacheIsDir",
                         "Configured cache file \"{0}\" is a directory.",
                         new Object[] {cacheFile.getPath()});
                }
            }
        }

        else if (varName.equals (VAR_DAYS_TO_CACHE))
        {
            defaultCacheDays = parseMaxDaysParameter (MAIN_SECTION,
                                                      varName,
                                                      DEF_DAYS_TO_CACHE);
            val = String.valueOf (defaultCacheDays);
        }

        else if (varName.equals (VAR_TOTAL_CACHE_BACKUPS))
        {
            totalCacheBackups =
                getOptionalCardinalValue (MAIN_SECTION,
                                          varName,
                                          DEF_TOTAL_CACHE_BACKUPS);
            val = String.valueOf (totalCacheBackups);
        }

        else if (varName.equals (VAR_NO_CACHE_UPDATE))
        {
            updateCache = (!getOptionalBooleanValue (MAIN_SECTION,
                                                     varName,
                                                     DEF_NO_CACHE_UPDATE));
            val = String.valueOf (updateCache);
        }

        else if (varName.equals (VAR_SUMMARY_ONLY))
        {
            summaryOnly = getOptionalBooleanValue (MAIN_SECTION,
                                                   varName,
                                                   DEF_SUMMARY_ONLY);
            val = String.valueOf (summaryOnly);
        }

        else if (varName.equals (VAR_MAX_SUMMARY_SIZE))
        {
            maxSummarySize = getOptionalCardinalValue (MAIN_SECTION,
                                                       varName,
                                                       DEF_MAX_SUMMARY_SIZE);
            val = String.valueOf (maxSummarySize);
        }

        else if (varName.equals (VAR_SHOW_RSS_VERSION))
        {
            showRSSFormat = getOptionalBooleanValue (MAIN_SECTION,
                                                     varName,
                                                     DEF_SHOW_RSS_VERSION);
            val = String.valueOf (showRSSFormat);
        }

        else if (varName.equals (VAR_SHOW_DATES))
        {
            showDates = getOptionalBooleanValue (MAIN_SECTION,
                                                 varName,
                                                 DEF_SHOW_DATES);
            val = String.valueOf (showDates);
        }

        else if (varName.equals (VAR_PARSER_CLASS_NAME))
        {
            parserClassName = getOptionalStringValue (MAIN_SECTION,
                                                      varName,
                                                      DEF_PARSER_CLASS_NAME);
            val = String.valueOf (parserClassName);
        }

        else if (varName.equals (VAR_SMTP_HOST))
        {
            smtpHost = getOptionalStringValue (MAIN_SECTION,
                                               varName,
                                               DEF_SMTP_HOST);
            val = smtpHost;
        }

        else if (varName.equals (VAR_EMAIL_SENDER))
        {
            emailSender = getOptionalStringValue (MAIN_SECTION,
                                                  varName,
                                                  null);
            val = emailSender;
        }

        else if (varName.equals (VAR_EMAIL_SUBJECT))
        {
            emailSubject = getOptionalStringValue (MAIN_SECTION,
                                                   varName,
                                                   DEF_EMAIL_SUBJECT);
            val = emailSubject;
        }

        else if (varName.equals (VAR_SHOW_AUTHORS))
        {
            showAuthors = getOptionalBooleanValue (MAIN_SECTION,
                                                   varName,
                                                   DEF_SHOW_AUTHORS);
            val = String.valueOf (showAuthors);
        }

        else if (varName.equals (VAR_ALLOW_EMBEDDED_HTML))
        {
            allowEmbeddedHTML =
                getOptionalBooleanValue (MAIN_SECTION,
                                         varName,
                                         DEF_ALLOW_EMBEDDED_HTML);
            val = String.valueOf (allowEmbeddedHTML);
        }

        else if (varName.equals (VAR_GET_GZIPPED_FEEDS))
        {
            getGzippedFeeds = getOptionalBooleanValue (MAIN_SECTION,
                                                       varName,
                                                       DEF_GET_GZIPPED_FEEDS);
            val = String.valueOf (getGzippedFeeds);
        }

        else if (varName.equals (VAR_MAX_THREADS))
        {
            int maxThreads = getOptionalCardinalValue (MAIN_SECTION,
                                                       varName,
                                                       DEF_MAX_THREADS);
            setMaxThreads (maxThreads);
            val = String.valueOf (maxThreads);
        }

        else if (varName.equals (VAR_USER_AGENT))
        {
            defaultUserAgent = getOptionalStringValue (MAIN_SECTION,
                                                       varName,
                                                       null);
            val = defaultUserAgent;
        }

        else
        {
            val = getOptionalStringValue (MAIN_SECTION, varName, null);
        }

        if (val != null)
        {
            MetaPlugIn.getMetaPlugIn().runMainConfigItemPlugIn (MAIN_SECTION,
                                                                varName,
                                                                this);
        }

        if (defaultUserAgent == null)
        {
            StringBuilder buf = new StringBuilder();

            // Standard format seems to be:
            //
            // tool/version (+url)
            //
            // e.g.: Googlebot/2.1 (+http://www.google.com/bot.htm

            buf.append (Version.getUtilityName());
            buf.append ('/');
            buf.append (Version.getVersionNumber());
            buf.append (" (+");
            buf.append (Version.getWebSite());
            buf.append (')');
            defaultUserAgent = buf.toString();
        }

        val = defaultUserAgent;
    }

    /**
     * Process a section that identifies an RSS feed to be polled.
     *
     * @param sectionName  the section name
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void processFeedSection (String sectionName)
        throws ConfigurationException,
               CurnException
    {
        FeedInfo           feedInfo = null;
        String             feedURLString = null;
        Collection<String> preparseEditCommands = new ArrayList<String>();
        URL                url = null;
        MetaPlugIn         metaPlugIn = MetaPlugIn.getMetaPlugIn();

        feedURLString = getConfigurationValue (sectionName, VAR_FEED_URL);

        try
        {
            url = Util.normalizeURL (feedURLString);
            String urlString = url.toString();
            log.debug ("Configured feed: URL=\"" + urlString + "\"");
            feedInfo = new FeedInfo (url);
            metaPlugIn.runFeedConfigItemPlugIn (sectionName,
                                                VAR_FEED_URL,
                                                this,
                                                feedInfo);
        }

        catch (MalformedURLException ex)
        {
            throw new ConfigurationException (Constants.BUNDLE_NAME,
                                              "CurnConfig.badFeedURL",
                                              "Configuration file section "
                                            + "\"{0}\" specifies a bad RSS "
                                            + "feed URL \"{1}\"",
                                              new Object[]
                                              {
                                                  sectionName,
                                                  feedURLString
                                              });
        }


        feedInfo.setPruneURLsFlag (DEF_PRUNE_URLS);
        feedInfo.setDaysToCache (defaultCacheDays);
        feedInfo.setSummarizeOnlyFlag (summaryOnly);
        feedInfo.setUserAgent (defaultUserAgent);
        feedInfo.setMaxSummarySize (maxSummarySize);
        feedInfo.setShowAuthorsFlag (showAuthors);
        feedInfo.setAllowEmbeddedHTMLFlag (allowEmbeddedHTML);

        for (String varName : getVariableNames (sectionName))
        {
            String value   = null;
            boolean flag;

            if (varName.equals (VAR_DAYS_TO_CACHE))
            {
                int maxDays = parseMaxDaysParameter (sectionName,
                                                     varName,
                                                     defaultCacheDays);
                feedInfo.setDaysToCache (maxDays);
                value = String.valueOf (maxDays);
            }

            else if (varName.equals (VAR_PRUNE_URLS))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setPruneURLsFlag (flag);
                value = String.valueOf (flag);
            }

            else if (varName.equals (VAR_SUMMARY_ONLY))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setSummarizeOnlyFlag (flag);
                value = String.valueOf (flag);
            }

            else if (varName.equals (VAR_MAX_SUMMARY_SIZE))
            {
                int maxSummarySize = getRequiredCardinalValue (sectionName,
                                                               varName);
                feedInfo.setMaxSummarySize (maxSummarySize);
                value = String.valueOf (maxSummarySize);
            }

            else if (varName.equals (VAR_DISABLED))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setEnabledFlag (! flag);
                value = String.valueOf (flag);
            }

            else if (varName.equals (VAR_IGNORE_DUP_TITLES))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setIgnoreItemsWithDuplicateTitlesFlag (flag);
                value = String.valueOf (flag);
            }

            else if (varName.equals (VAR_TITLE_OVERRIDE))
            {
                value = getConfigurationValue (sectionName, varName);
                feedInfo.setTitleOverride (value);
            }

            else if (varName.equals (VAR_EDIT_ITEM_URL))
            {
                value = getConfigurationValue (sectionName, varName);
                feedInfo.setItemURLEditCommand (value);
            }

            else if (varName.equals (VAR_FORCE_ENCODING) ||
                     varName.equals (VAR_FORCE_CHAR_ENCODING))
            {
                value = getConfigurationValue (sectionName, varName);
                feedInfo.setForcedCharacterEncoding (value);
            }

            else if (varName.startsWith (VAR_PREPARSE_EDIT))
            {
                value = getConfigurationValue (sectionName, varName);
                preparseEditCommands.add (value);
            }

            else if (varName.equals (VAR_USER_AGENT))
            {
                value = getConfigurationValue (sectionName, varName);
                feedInfo.setUserAgent (value);
            }

            else if (varName.equals (VAR_SHOW_AUTHORS))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setShowAuthorsFlag (flag);
                value = String.valueOf (flag);
            }

            else if (varName.equals (VAR_ALLOW_EMBEDDED_HTML))
            {
                flag = getRequiredBooleanValue (sectionName, varName);
                feedInfo.setAllowEmbeddedHTMLFlag (flag);
                value = String.valueOf (flag);
            }

            else
            {
                value = getConfigurationValue (sectionName, varName);
            }

            if (value != null)
            {
                metaPlugIn.runFeedConfigItemPlugIn (sectionName,
                                                    varName,
                                                    this,
                                                    feedInfo);
            }
        }

        if (preparseEditCommands.size() > 0)
        {
            String[] cmds = new String[preparseEditCommands.size()];
            cmds = (String[]) preparseEditCommands.toArray (cmds);
            feedInfo.setPreparseEditCommands (cmds);
        }

        if (url == null)
        {
            throw new ConfigurationException (Constants.BUNDLE_NAME,
                                              "CurnConfig.missingReqVar",
                                              "The configuration file is "
                                            + "missing required variable "
                                            + "\"{0}\" in section \"{1}\"",
                                              new Object[]
                                              {
                                                  VAR_FEED_URL,
                                                  sectionName
                                              });
        }

        feeds.add (feedInfo);
        feedMap.put (url.toString(), feedInfo);
    }

    /**
     * Process a section that identifies an output handler.
     *
     * @param sectionName  the section name
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void processOutputHandlerSection (String sectionName)
        throws ConfigurationException,
               CurnException
    {
        // Get the required class name.

        String                  className;
        ConfiguredOutputHandler handlerWrapper;
        MetaPlugIn              metaPlugIn = MetaPlugIn.getMetaPlugIn();

        className = getConfigurationValue (sectionName, VAR_CLASS);
        handlerWrapper = new ConfiguredOutputHandler (sectionName,
                                                      sectionName,
                                                      className);
        metaPlugIn.runOutputHandlerConfigItemPlugIn (sectionName,
                                                     VAR_CLASS,
                                                     this,
                                                     handlerWrapper);
        
        // Only process the rest if it's not disabled.

        boolean disabled = false;
        String value = getOptionalStringValue (sectionName,
                                               VAR_DISABLED,
                                               null);
        if (value != null)
        {
            disabled = getOptionalBooleanValue (sectionName,
                                                VAR_DISABLED,
                                                false);
            metaPlugIn.runOutputHandlerConfigItemPlugIn (sectionName,
                                                         VAR_DISABLED,
                                                         this,
                                                         handlerWrapper);
        }

        if (! disabled)
        {
            for (String variableName : getVariableNames (sectionName))
            {
                // Skip the ones we've already processed.

                if (variableName.equals (VAR_DISABLED))
                    continue;

                if (variableName.equals (VAR_CLASS))
                    continue;

                value = getConfigurationValue (sectionName, variableName);
                handlerWrapper.addExtraVariable (variableName, value);

                metaPlugIn.runOutputHandlerConfigItemPlugIn (sectionName,
                                                             variableName,
                                                             this,
                                                             handlerWrapper);
            }

            log.debug ("Saving output handler \""
                     + handlerWrapper.getName()
                     + "\" of type "
                     + handlerWrapper.getClassName());
            outputHandlers.add (handlerWrapper);
        }
    }

    /**
     * Process an unknown section (passing its values to the plug-ins).
     *
     * @param sectionName  the section name
     *
     * @throws ConfigurationException  configuration error
     * @throws CurnException           some other error
     */
    private void processUnknownSection (String sectionName)
        throws ConfigurationException,
               CurnException
    {
        for (String varName : getVariableNames (sectionName))
        {
            String value = getConfigurationValue (sectionName, varName);
            if (value != null)
            {
                MetaPlugIn.getMetaPlugIn().runUnknownSectionConfigItemPlugIn
                    (sectionName, varName, this);
            }
        }
    }

    /**
     * Parse an optional MaxDaysToCache parameter.
     *
     * @param sectionName   the section name
     * @param variableName  the variable name
     * @param def           the default
     *
     * @return the value
     *
     * @throws NoSuchSectionException no such section
     * @throws ConfigurationException bad numeric value
     */
    private int parseMaxDaysParameter (String sectionName,
                                       String variableName,
                                       int    def)
        throws NoSuchSectionException,
               ConfigurationException
    {
        int result = def;
        String value = getOptionalStringValue (sectionName,
                                               variableName,
                                               null);
        if (value != null)
        {
            if (value.equalsIgnoreCase (NO_LIMIT_VALUE))
                result = Integer.MAX_VALUE;

            else
            {
                try
                {
                    result = Integer.parseInt (value);
                }

                catch (NumberFormatException ex)
                {
                    throw new ConfigurationException
                                         (Constants.BUNDLE_NAME,
                                          "CurnConfig.badNumericValue",
                                          "Bad numeric value \"{0}\" for "
                                        + "variable \"{1}\" in section "
                                        + "\"{2}\"",
                                          new Object[]
                                          {
                                              value,
                                              variableName,
                                              sectionName
                                          });
                }

                if (result < 0)
                {
                    throw new ConfigurationException
                                      (Constants.BUNDLE_NAME,
                                       "CurnConfig.negativeCardinalValue",
                                       "Unexpected negative numeric value "
                                     + "{0} for variable \"{1}\" in section "
                                     + "\"{2}\"",
                                       new Object[]
                                       {
                                           value,
                                           variableName,
                                           sectionName
                                       });
                }
            }
        }

        return result;
    }
}
