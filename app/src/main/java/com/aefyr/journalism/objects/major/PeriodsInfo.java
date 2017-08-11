package com.aefyr.journalism.objects.major;

import com.aefyr.journalism.objects.minor.ActualPeriod;
import com.aefyr.journalism.objects.minor.AmbigiousPeriod;

import java.util.ArrayList;


public class PeriodsInfo {
	ArrayList<ActualPeriod> periods;
	ArrayList<AmbigiousPeriod> ambigiousPeriods;
	
	PeriodsInfo(){
		periods = new ArrayList<ActualPeriod>();
		ambigiousPeriods = new ArrayList<AmbigiousPeriod>();
	}
	
	public ArrayList<ActualPeriod> getPeriods(){
		return periods;
	}
	
	public ArrayList<AmbigiousPeriod> getAmbigiousPeriods(){
		return ambigiousPeriods;
	}
	
	void addPeriod(ActualPeriod period){
		periods.add(period);
	}
	
	void addAmbigiousPeriod(AmbigiousPeriod ambigiousPeriod){
		ambigiousPeriods.add(ambigiousPeriod);
	}

}
