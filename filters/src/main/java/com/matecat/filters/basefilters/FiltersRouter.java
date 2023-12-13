package com.matecat.filters.basefilters;

import com.matecat.converter.core.XliffProcessor;
import com.matecat.converter.core.util.Config;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiltersRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FiltersRouter.class);

    private List<IFilter> filters;

    public FiltersRouter() {
        filters = new ArrayList<>();
        for (Class<IFilter> filter : Config.CUSTOM_FILTERS) {
            try {
                filters.add(filter.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Exception instantiating filter " + filter, e);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(FiltersRouter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public File extract(File sourceFile, Locale sourceLanguage, Locale targetLanguage, String segmentation) {
        for (IFilter filter : filters) {
            if (filter.isSupported(sourceFile)) {
                if (!(filter instanceof DefaultFilter)) {
                    LOGGER.info("Using custom filter: " + filter.getClass().getCanonicalName());
                }
                return filter.extract(sourceFile, sourceLanguage, targetLanguage, segmentation);
            }
        }
        throw new IllegalStateException("No registered filter supports the source file");
    }

    public File merge(File xliff) {
        XliffProcessor processor = new XliffProcessor(xliff);
        String filterName = processor.getFilter();
        if (filterName == null) {
            LOGGER.warn("Missing filter class name in XLIFF: using DefaultFilter");
            filterName = DefaultFilter.class.getCanonicalName();
        }
        IFilter filter = null;
        try {
            Class<IFilter> filterClass = (Class<IFilter>) Class.forName(filterName);
            if (!filterClass.equals(DefaultFilter.class)) {
                LOGGER.info("Using custom filter: " + filterName);
            }
            filter = filterClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Exception while loading filter class " + filterName, e);
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            java.util.logging.Logger.getLogger(FiltersRouter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return (filter != null) ? filter.merge(processor) : null;
    }
}
