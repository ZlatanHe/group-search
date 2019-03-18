package com.zlatan.interview.ant.fin.group.search.output;

import java.util.Collection;

/**
 * Created by Zlatan on 19/3/18.
 */
public interface Printer {

    void print(Printable printable);

    void print(Collection<? extends Printable> printableCollection);
}
