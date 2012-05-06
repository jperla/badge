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

    // Construct a VCard from a JSON repr stringesentation.
    public VCard(String json_string) {
        this();

        try {

            JSONObject jo = new JSONObject(json_string);

            bachelors = jo.getBoolean("bachelors");
            if (bachelors) {
                bachelors_school = jo.getString("bachelors_school");
                bachelors_gradyear = jo.getInt("bachelors_gradyear");
                bachelors_advisors = JSONArray_to_ArrayListString(jo, "bachelors_advisors");
            }

            masters = jo.getBoolean("masters");
            if (masters) {
                masters_school = jo.getString("masters_school");
                masters_gradyear = jo.getInt("masters_gradyear");
                masters_advisors = JSONArray_to_ArrayListString(jo, "masters_advisors");
            }

            phd = jo.getBoolean("phd");
            if (phd) {
                phd_school = jo.getString("phd_school");
                phd_gradyear = jo.getInt("phd_gradyear");
                phd_advisors = JSONArray_to_ArrayListString(jo, "phd_advisors");
            }

            JSONArray jobs_arr = jo.getJSONArray("jobs");
            for (int i = 0; i < jobs_arr.length(); i++) {
                JSONObject job_obj = jobs_arr.getJSONObject(i);
                jobs.add(new Job(job_obj.getString("company"),
                                 job_obj.getString("title"),
                                 job_obj.getInt("start_year"),
                                 job_obj.getInt("end_year")));
            }

            talks_attended     = JSONArray_to_ArrayListString(jo, "talks_attended");
            talks_attending    = JSONArray_to_ArrayListString(jo, "talks_attending");
            talks_given        = JSONArray_to_ArrayListString(jo, "talks_given");
            talks_giving       = JSONArray_to_ArrayListString(jo, "talks_giving");
            research_interests = JSONArray_to_ArrayListString(jo, "research_interests");

        }
        catch(JSONException je) {}
    }

    public static ArrayList<String> JSONArray_to_ArrayListString(
        JSONObject jo, String arr_name) throws JSONException
    {
        ArrayList<String> al = new ArrayList<String>();
        JSONArray arr = jo.getJSONArray(arr_name);
        for (int i = 0; i < arr.length(); i++) {
            al.add(arr.getString(i));
        }

        return al;
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
                jo.put("bachelors_advisors", new JSONArray(bachelors_advisors));
            }

            jo.put("masters", masters);
            if (masters) {
                jo.put("masters_school", masters_school);
                jo.put("masters_gradyear", masters_gradyear);
                jo.put("masters_advisors", new JSONArray(masters_advisors));
            }

            jo.put("phd", phd);
            if (phd) {
                jo.put("phd_school", phd_school);
                jo.put("phd_gradyear", phd_gradyear);
                jo.put("phd_advisors", new JSONArray(phd_advisors));
            }

            jo.put("jobs", new JSONArray());
            for (Job j : jobs) {
                JSONObject job_obj = new JSONObject();
                job_obj.put("company",    j.company);
                job_obj.put("title",      j.title);
                job_obj.put("start_year", j.start_year);
                job_obj.put("end_year",   j.end_year);

                jo.append("jobs", job_obj);
            }

            jo.put("talks_attended", talks_attended);
            jo.put("talks_attending", talks_attending);
            jo.put("talks_given", talks_given);
            jo.put("talks_giving", talks_giving);
            jo.put("research_interests", research_interests);
        }
        catch (JSONException je) { }

        return jo;
    }

    // Just for testing.
    public static void main(String args[]) throws JSONException
    {
        VCard v = getBrandon();
        String s = v.toJSON().toString();
        System.out.println(s + "\n");
        VCard v2 = new VCard(s);
        String s2 = v2.toJSON().toString();
        System.out.println(s2 + "\n");
        if (s.equals(s2)) System.out.println("SUCCESS!");
        else System.out.println("FAILURE");
    }

}

