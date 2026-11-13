package com.hms.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes every module (table) of the application. One generic servlet
 * (ModuleServlet) and generic JSP fragments render list/add/edit/view/delete
 * pages from these definitions. To add a field: add the column in SQL and
 * one add(...) line below.
 */
public class Entities {

    /** A single column / form field. */
    public static class Field {
        private final String name;
        private final String label;
        private final String type;   // text, email, number, date, time, textarea, select, ref, password
        private boolean required = true;
        private boolean list = true;
        private boolean search = false;
        private boolean computed = false;
        private boolean today = false;
        private String def = "";
        private String[] options = new String[0];
        private String refTable;
        private String refLabel;

        Field(String name, String label, String type) {
            this.name = name;
            this.label = label;
            this.type = type;
        }

        Field opt() { required = false; return this; }
        Field hide() { list = false; return this; }
        Field search() { search = true; return this; }
        Field calc() { computed = true; required = false; return this; }
        Field today() { today = true; required = false; return this; }
        Field def(String d) { def = d; return this; }
        Field opts(String csv) { options = csv.split(","); return this; }
        Field ref(String table, String labelExpr) { refTable = table; refLabel = labelExpr; return this; }

        public String getName() { return name; }
        public String getLabel() { return label; }
        public String getType() { return type; }
        public boolean isRequired() { return required; }
        public boolean isList() { return list; }
        public boolean isSearch() { return search; }
        public boolean isComputed() { return computed; }
        public boolean isToday() { return today; }
        public String getDef() { return def; }
        public String[] getOptions() { return options; }
        public String getRefTable() { return refTable; }
        public String getRefLabel() { return refLabel; }
        /** Column alias holding the display text of a reference field. */
        public String getLabelKey() { return name + "_label"; }
    }

    /** A module = one database table with its pages. */
    public static class Module {
        private final String key;
        private final String title;
        private final String singular;
        private final Set<String> roles;
        private final Set<String> writeRoles;
        private String orderBy = "t.id DESC";
        private String doctorCol;
        private boolean allowCreate = true;
        private boolean allowEdit = true;
        private final List<Field> fields = new ArrayList<Field>();

        Module(String key, String title, String singular, String roles, String writeRoles) {
            this.key = key;
            this.title = title;
            this.singular = singular;
            this.roles = new HashSet<String>(Arrays.asList(roles.split(",")));
            this.writeRoles = new HashSet<String>(Arrays.asList(writeRoles.split(",")));
        }

        Module add(Field f) { fields.add(f); return this; }
        Module order(String o) { orderBy = o; return this; }
        Module doctorCol(String c) { doctorCol = c; return this; }
        Module readOnlyData() { allowCreate = false; allowEdit = false; return this; }

        public String getKey() { return key; }
        public String getTable() { return key; }
        public String getTitle() { return title; }
        public String getSingular() { return singular; }
        public String getOrderBy() { return orderBy; }
        /** Column that links a row to a doctor (rows are filtered for doctor logins). */
        public String getDoctorCol() { return doctorCol; }
        public boolean isAllowCreate() { return allowCreate; }
        public boolean isAllowEdit() { return allowEdit; }
        public List<Field> getFields() { return fields; }
        public boolean canAccess(String role) { return roles.contains(role); }
        public boolean canWrite(String role) { return writeRoles.contains(role); }
    }

    private static final Map<String, Module> MODULES = new LinkedHashMap<String, Module>();

    private static Field text(String n, String l) { return new Field(n, l, "text"); }
    private static Field email(String n, String l) { return new Field(n, l, "email"); }
    private static Field number(String n, String l) { return new Field(n, l, "number"); }
    private static Field date(String n, String l) { return new Field(n, l, "date"); }
    private static Field time(String n, String l) { return new Field(n, l, "time"); }
    private static Field area(String n, String l) { return new Field(n, l, "textarea").hide(); }
    private static Field select(String n, String l, String csv) { return new Field(n, l, "select").opts(csv); }
    private static Field patient() { return new Field("patient_id", "Patient", "ref").ref("patients", "name || ' (ID ' || id || ')'"); }
    private static Field doctor() { return new Field("doctor_id", "Doctor", "ref").ref("doctors", "'Dr. ' || name"); }

    private static void register(Module m) { MODULES.put(m.getKey(), m); }

    static {
        register(new Module("patients", "Patients", "Patient", "admin,receptionist,doctor", "admin,receptionist")
            .add(text("name", "Full Name").search())
            .add(number("age", "Age"))
            .add(select("gender", "Gender", "Male,Female,Other"))
            .add(text("phone", "Phone").search())
            .add(email("email", "Email").opt().hide())
            .add(select("blood_group", "Blood Group", "A+,A-,B+,B-,O+,O-,AB+,AB-").opt())
            .add(area("address", "Address").opt())
            .add(date("registered_on", "Registered On").today()));

        register(new Module("doctors", "Doctors", "Doctor", "admin,receptionist,doctor", "admin")
            .add(text("name", "Name").search())
            .add(text("specialization", "Specialization").search())
            .add(text("qualification", "Qualification").opt().hide())
            .add(text("phone", "Phone"))
            .add(email("email", "Email").opt().hide())
            .add(number("experience", "Experience (yrs)").opt())
            .add(number("fee", "Consultation Fee"))
            .add(text("available_days", "Available Days").opt()));

        register(new Module("appointments", "Appointments", "Appointment", "admin,receptionist,doctor", "admin,receptionist,doctor")
            .order("t.appt_date DESC, t.appt_time DESC").doctorCol("doctor_id")
            .add(patient().search())
            .add(doctor().search())
            .add(date("appt_date", "Date"))
            .add(time("appt_time", "Time"))
            .add(text("reason", "Reason").opt())
            .add(select("status", "Status", "Scheduled,Completed,Cancelled").def("Scheduled")));

        register(new Module("medical_records", "Medical Records", "Medical Record", "admin,doctor", "admin,doctor")
            .order("t.visit_date DESC, t.id DESC").doctorCol("doctor_id")
            .add(patient().search())
            .add(doctor())
            .add(date("visit_date", "Visit Date").today())
            .add(text("diagnosis", "Diagnosis").search())
            .add(area("symptoms", "Symptoms"))
            .add(area("treatment", "Treatment"))
            .add(area("notes", "Notes").opt()));

        register(new Module("prescriptions", "Prescriptions", "Prescription", "admin,doctor", "admin,doctor")
            .doctorCol("doctor_id")
            .add(patient().search())
            .add(doctor())
            .add(date("presc_date", "Date").today())
            .add(area("medicines", "Medicines (one per line: name - dosage - duration)"))
            .add(area("instructions", "Instructions").opt()));

        register(new Module("bills", "Billing", "Bill", "admin,receptionist", "admin,receptionist")
            .add(patient().search())
            .add(date("bill_date", "Bill Date").today())
            .add(number("consultation", "Consultation").opt().def("0"))
            .add(number("medicine", "Medicine").opt().def("0"))
            .add(number("lab", "Lab").opt().def("0"))
            .add(number("other", "Other").opt().def("0"))
            .add(number("total", "Total").calc())
            .add(select("status", "Status", "Pending,Paid").def("Pending")));

        register(new Module("medicines", "Pharmacy", "Medicine", "admin,receptionist", "admin,receptionist")
            .order("t.name ASC")
            .add(text("name", "Medicine").search())
            .add(text("category", "Category").search())
            .add(number("quantity", "Quantity"))
            .add(number("price", "Price"))
            .add(date("expiry_date", "Expiry Date"))
            .add(number("reorder_level", "Reorder Level").def("10")));

        register(new Module("lab_tests", "Laboratory", "Lab Test", "admin,receptionist,doctor", "admin,doctor")
            .doctorCol("doctor_id")
            .add(patient().search())
            .add(doctor())
            .add(text("test_name", "Test Name").search())
            .add(date("test_date", "Test Date").today())
            .add(number("cost", "Cost").opt())
            .add(select("status", "Status", "Pending,Completed").def("Pending"))
            .add(area("result", "Result").opt()));

        register(new Module("users", "Staff Users", "User", "admin", "admin")
            .order("t.id ASC")
            .add(text("username", "Username").search())
            .add(text("full_name", "Full Name").search())
            .add(email("email", "Email"))
            .add(select("role", "Role", "admin,doctor,receptionist"))
            .add(new Field("doctor_id", "Linked Doctor", "ref").ref("doctors", "'Dr. ' || name").opt())
            .add(new Field("password_hash", "Password", "password").hide()));

        register(new Module("messages", "Contact Messages", "Message", "admin", "admin")
            .readOnlyData()
            .add(text("name", "Name").search())
            .add(email("email", "Email").search())
            .add(text("subject", "Subject").search())
            .add(area("message", "Message"))
            .add(text("created_at", "Received")));
    }

    private Entities() { }

    public static Module get(String key) {
        return key == null ? null : MODULES.get(key);
    }

    public static Collection<Module> all() {
        return MODULES.values();
    }

    /** Modules shown in the sidebar for a role. */
    public static List<Module> menuFor(String role) {
        List<Module> out = new ArrayList<Module>();
        for (Module m : MODULES.values()) {
            if (m.canAccess(role)) {
                out.add(m);
            }
        }
        return out;
    }
}
