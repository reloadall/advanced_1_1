public enum Command {
    ZIP("zip"),
    DOWNLOAD("download"),
    SUM("sum");

    private final String id;

    Command(String id) {
        this.id = id;
    }

    public String getId(){
        return id;
    }
}
