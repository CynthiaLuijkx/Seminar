package Tools;

import java.util.Comparator;

public class SortByContractHours implements Comparator<ContractGroup> 
{ 
    public int compare(ContractGroup a, ContractGroup b) 
    { 
        return (int) ((a.getAvgHoursPerDay() * a.getAvgDaysPerWeek()) - (b.getAvgHoursPerDay() * b.getAvgDaysPerWeek())); 
    } 

}
