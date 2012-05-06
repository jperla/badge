package com.jperla.badge;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class VCard {

    private static class Job {
        public String company;
        public String title;
        public int start_year;
        public int end_year;

        public Job(String company, String title,
                   int start_year, int end_year) {
            this.company = company;
            this.title = title;
            this.start_year = start_year;
            this.end_year = end_year;
        }
    }

    // Bachelor's info
    public boolean bachelors;
    public String bachelors_school;
    public int bachelors_gradyear;
    public ArrayList<String> bachelors_advisors;

    // Master's info
    public boolean masters;
    public String masters_school;
    public int masters_gradyear;
    public ArrayList<String> masters_advisors;

    // Ph. D. info
    public boolean phd;
    public String phd_school;
    public int phd_gradyear;
    public ArrayList<String> phd_advisors;

    // Jobs
    public ArrayList<Job> jobs;

    // Conference presentation info
    public ArrayList<String> talks_attended;
    public ArrayList<String> talks_attending;
    public ArrayList<String> talks_given;
    public ArrayList<String> talks_giving;

    // Research info
    public ArrayList<String> research_interests;

    // Generate a blank VCard.
    public VCard()
    {
        bachelors = masters = phd = false;

        bachelors_advisors = new ArrayList<String>();
        masters_advisors   = new ArrayList<String>();
        phd_advisors       = new ArrayList<String>();

        jobs               = new ArrayList<Job>();

        talks_attended     = new ArrayList<String>();
        talks_attending    = new ArrayList<String>();
        talks_given        = new ArrayList<String>();
        talks_giving       = new ArrayList<String>();

        research_interests = new ArrayList<String>();
    }

    // Get my card.
    public static VCard getBrandon()
    {
        VCard B = new VCard();

        B.bachelors = true;
        B.bachelors_school = "Princeton University";
        B.bachelors_gradyear = 2012;
        B.bachelors_advisors.add("Jennifer Rexford");
        B.bachelors_advisors.add("Michael Freedman");

        B.jobs.add(new Job("EarthColor", "Software Developer",
                           2009, 2009));
        B.jobs.add(new Job("NVIDIA", 
                           "System Software Engineering Intern",
                           2010, 2011));

        B.talks_attended.add("ZebraNet Hardware Design");

        B.talks_attending.add("C-LINK");
        B.talks_attending.add("Eon");

        B.talks_giving.add("Why I Have My Own UI Guy");

        B.research_interests.add("Operating Systems");
        B.research_interests.add("Computer Networking");
        B.research_interests.add("Virtual Worlds");

        return B;
    }

    // Convert to JSON representation to send over network.
    public JSONObject toJSON()
    {
        JSONObject jo = new JSONObject();

        try {
            jo.put("bachelors", bachelors);
            if (bachelors) {
                jo.put("bachelors_school", bachelors_school);
                jo.put("bachelors_gradyear", bachelors_gradyear);
                for (String s : bachelors_advisors) {
                    jo.append("bachelors_advisors", s);
                }
            }

            jo.put("masters", masters);
            if (masters) {
                jo.put("masters_school", masters_school);
                jo.put("masters_gradyear", masters_gradyear);
                for (String s : masters_advisors) {
                    jo.append("masters_advisors", s);
                }
            }

            jo.put("phd", phd);
            if (phd) {
                jo.put("phd_school", phd_school);
                jo.put("phd_gradyear", phd_gradyear);
                for (String s : phd_advisors) {
                    jo.append("phd_advisors", s);
                }
            }

            for (Job j : jobs) {
                JSONObject job_obj = new JSONObject();
                job_obj.put("company",    j.company);
                job_obj.put("title",      j.title);
                job_obj.put("start_year", j.start_year);
                job_obj.put("end_year",   j.end_year);

                jo.append("jobs", job_obj);
            }

            for (String s : talks_attended) {
                jo.append("talks_attended", s);
            }

            for (String s : talks_attending) {
                jo.append("talks_attending", s);
            }

            for (String s : talks_given) {
                jo.append("talks_given", s);
            }

            for (String s : talks_giving) {
                jo.append("talks_giving", s);
            }

            for (String s : research_interests) {
                jo.append("research_interests", s);
            }
        }
        catch (JSONException je) { }

        return jo;
    }

    // Just for testing.
    public static void main(String args[]) throws JSONException
    {
        VCard v = getBrandon();
        System.out.println(v.toJSON().toString());
    }

}

