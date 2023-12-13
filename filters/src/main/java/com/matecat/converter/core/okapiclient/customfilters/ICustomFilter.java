package com.matecat.converter.core.okapiclient.customfilters;

import java.io.File;
import net.sf.okapi.common.filters.IFilter;

public interface ICustomFilter {

    IFilter getFilter(File file);
}
