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

    public static VCard getZhao()
    {
        VCard Z = new VCard();

        Z.bachelors = true;
        Z.bachelors_school = "Princeton University";
        Z.bachelors_gradyear = 2012;
        Z.bachelors_advisors.add("Szymon Rusinkiewicz");

        Z.jobs.add(new Job("Google", 
                           "Software Engineering Intern",
                           2011, 2011));

        Z.talks_attended.add("Javascript: The Good Parts");
        Z.talks_attended.add("ZebraNet Hardware Design");

        Z.research_interests.add("Computer Vision");
        Z.research_interests.add("Computer Graphics");
        Z.research_interests.add("Virtual Worlds");

        return Z;
    }

    public static String extractCommonalities(VCard c1, VCard c2) {
        String s = "";
        ArrayList<String> common;

        if (c1.bachelors && c2.bachelors) {
            if (sameString(c1.bachelors_school, c2.bachelors_school)) {
                s += "Attended " + c1.bachelors_school + " for Bachelor's\n\n";
            }
            common = intersectList(c1.bachelors_advisors, c2.bachelors_advisors);
            if (common.size() > 0) {
                s += "Worked with " + stringify(common) + "\n\n";
            }
        }

        // TODO: same for master's and phd

        common = intersectJobs(c1.jobs, c2.jobs);
        if (common.size() > 0) {
            s += "Worked at " + stringify(common) + "\n\n";
        }

        common = intersectList(c1.talks_attended, c2.talks_attended);
        if (common.size() > 0) {
            s += "Attended the " + stringify(common) + " talks\n\n";
        }
        
        common = intersectList(c1.talks_attending, c2.talks_attending);
        if (common.size() > 0) {
            s += "Attending the " + stringify(common) + " talks\n\n";
        }

        common = intersectList(c1.talks_given, c2.talks_given);
        if (common.size() > 0) {
            s += "Attending the " + stringify(common) + " talks\n\n";
        }

        common = intersectList(c1.research_interests, c2.research_interests);
        if (common.size() > 0) {
            s += "Interested in these research areas: " + stringify(common) + "\n\n";
        }

        return s;
    }

    private static String stringify(ArrayList<String> l) {
        switch(l.size()) {
            case 0: return "";
            case 1: return l.get(0);
            case 2: return l.get(0) + " and " + l.get(1);
            default:
                String str = "";
                for (int i = 0; i < l.size() - 1; i++) {
                    str = l.get(i) + ", ";
                }
                str += "and " + l.get(l.size() - 1);
                return str;
        }
    }

    private static boolean sameString(String s1, String s2) {
        return (s1 != null) && s1.equals(s2);
    }

    private static ArrayList intersectList(ArrayList l1, ArrayList l2) {
        ArrayList result = new ArrayList(l1);
        result.retainAll(l2);
        return result;
    }

    private static ArrayList intersectJobs(ArrayList<Job> l1, ArrayList<Job> l2) {
        ArrayList<String> result = new ArrayList<String>();
        for (Job j1 : l1) {
            for (Job j2 : l2) {
                if (j1.company.equals(j2.company)) {
                    result.add(j1.company);
                    break;
                }
            }
        }
        return result;
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

            JSONArray jobs_arr = new JSONArray();
            for (Job j : jobs) {
                JSONObject job_obj = new JSONObject();
                job_obj.put("company",    j.company);
                job_obj.put("title",      j.title);
                job_obj.put("start_year", j.start_year);
                job_obj.put("end_year",   j.end_year);

                jobs_arr.put(job_obj);
            }
            jo.put("jobs", jobs_arr);

            jo.put("talks_attended",     new JSONArray(talks_attended));
            jo.put("talks_attending",    new JSONArray(talks_attending));
            jo.put("talks_given",        new JSONArray(talks_given));
            jo.put("talks_giving",       new JSONArray(talks_giving));
            jo.put("research_interests", new JSONArray(research_interests));
        }
        catch (JSONException je) { }

        return jo;
    }

    // Just for testing.
    public static void main(String args[]) throws JSONException
    {
        VCard v = getBrandon();
        // Test converting to json and back
        String s = v.toJSON().toString();
        System.out.println(s + "\n");
        VCard v2 = new VCard(s);
        String s2 = v2.toJSON().toString();
        System.out.println(s2 + "\n");
        if (s.equals(s2)) System.out.println("successfully converted to json and back!");
        else System.out.println("converting to json and back does not match original");

        // Test list intersection
        VCard z = getZhao();
        System.out.println("Zhao's card: \n" + z.toJSON().toString() + "\n");
        String str = VCard.extractCommonalities(v, z);
        System.out.println("Zhao and Brandon's commonalities: \n" + str);
    }

}

