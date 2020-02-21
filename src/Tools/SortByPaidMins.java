package Tools;

import java.util.Comparator;

public class SortByPaidMins implements Comparator<Duty> 
{ 
    public int compare(Duty a, Duty b) 
    { 
        return a.getPaidMin() - b.getPaidMin(); 
    } 
} 
