package com.matecat.converter.core.okapiclient.customfilters;

import com.matecat.converter.core.util.Config;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.okapi.common.filters.IFilter;

public class CustomFiltersRouter implements ICustomFilter {

    private List<ICustomFilter> customFilters;

    public CustomFiltersRouter() {
        customFilters = new ArrayList<>();
        for (Class<ICustomFilter> customFilter : Config.CUSTOM_FILTERS) {
            try {
                customFilters.add(customFilter.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating the custom filter " + customFilter, e);
            } catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(CustomFiltersRouter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public IFilter getFilter(File file) {
        IFilter chosenFilter = null;
        for (ICustomFilter customFilter : customFilters) {
            chosenFilter = customFilter.getFilter(file);
            if (chosenFilter != null) {
                break;
            }
        }
        return chosenFilter;
    }
}
