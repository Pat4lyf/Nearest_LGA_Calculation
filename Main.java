import com.google.common.collect.ImmutableList;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Tuple;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class Main {
    private static List<Tuple> list = ImmutableList.of(
            new Quartet<>("Aba North", "Abia", 5.33333333, 7.31666667),
            new Quartet<>("Aba South", "Abia", 5.1, 7.35),
            new Quartet<>("Arochukwu", "Abia", 5.38333333, 7.91666667),
            new Quartet<>("Bende", "Abia", 5.56666667, 7.63333333),
            new Quartet<>("Ikwuano", "Abia", 5.43333333, 7.56666667),
            new Quartet<>("Isiala Ngwa North", "Abia", 5.388966, 7.446948),
            new Quartet<>("Isiala Ngwa South", "Abia", 5.36241, 7.39998),
            new Quartet<>("Isuikwuato", "Abia", 5.533333, 7.483333),
            new Quartet<>("Obi Ngwa", "Abia", 5.155402, 7.457067),
            new Quartet<>("Ohafia", "Abia", 5.61666667, 7.83333333)
    );
    public static List<Tuple> readFromFile() {
        List<Tuple> list = new ArrayList<>();
        File file = new File("resources/lgas.csv");
        String line;

        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            while ((line = bf.readLine()) != null) {
                String[] arr = line.split(",");
                if (Objects.equals(arr[0], "id")) continue;
                list.add(new Quartet<>(arr[1], arr[4], Double.valueOf(arr[8]), Double.valueOf(arr[9])));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public static List<String> readFromFile1() {
        List<String> list = new ArrayList<>();
        File file = new File("resources/lgadb.csv");
        String line;

        try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
            while ((line = bf.readLine()) != null) {
                if (Objects.equals(line, "name")) continue;
                list.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public static boolean check() {
        List<String> lgaList1 = readFromFile().stream().map(l-> String.valueOf(l.getValue(0)).replace("'", "`")).collect(Collectors.toList());

        List<String> lgaList2 = readFromFile1().stream().map(l->l.replace("\"", "")).collect(Collectors.toList());

        List<String> match = new ArrayList<>();
        List<String> nonMatch = readFromFile().stream().map(l-> String.valueOf(l.getValue(0)).replace("'", "`")).collect(Collectors.toList());
        int count = 0;

        for (String s : lgaList1) {
            for (String value : lgaList2) {
                if (s.equalsIgnoreCase(value)) {
                    count++;
                    match.add(s);
                }
            }
        }

        for (String s: lgaList1) {
            for (String value : match) {
                if (s.equalsIgnoreCase(value)){
                    nonMatch.remove(s);
                }
            }
        }
        System.out.println(nonMatch.size());
        return count == lgaList1.size();

    }



    public static double distance(double lat1,
                                  double lat2, double lon1,
                                  double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r);
    }


    public static Quintet<String, String, String, String, String> getTopThreeNearestLgas(Quartet<String, String, Double, Double> lga, List<Tuple> list) {
        PriorityQueue<Pair<String, Double>> queue = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue1));

        List<Pair<String, Double>> result = list.stream()
                .filter(l-> !l.getValue(0).equals(lga.getValue0()))
                .map(l1-> new Pair<>((String) l1.getValue(0), distance(lga.getValue2(),
                        (double) l1.getValue(2), lga.getValue3(), (double) l1.getValue(3))))
                .collect(Collectors.toList());

        queue.addAll(result);

        return new Quintet<>(lga.getValue0(), queue.poll().getValue0(),queue.poll().getValue0(), queue.poll().getValue0(), lga.getValue1());


    }


    public static void generateResultUsingTheTotalLgas() {
        List<Tuple> readList = readFromFile();

        List<Quintet<String, String, String, String, String>> quintetList = readList
                .stream()
                .map(lga ->
                        getTopThreeNearestLgas((Quartet<String, String, Double, Double>) lga, readList)
                ).collect(Collectors.toList());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("nearestlgas.sql"))) {
            quintetList.forEach(list-> {
                String SQL = String.format
                        ("INSERT INTO nearest_lga_lookup (lga_name, first_nearest_lga, second_nearest_lga, third_nearest_lga, state_name) " +
                                "VALUES (\'%s\', \'%s\', \'%s\', \'%s\', \'%s\');\n", list.getValue0().toUpperCase().replace("'", "`"), list.getValue1().toUpperCase().replace("'", "`"), list.getValue2().toUpperCase().replace("'", "`"), list.getValue3().toUpperCase().replace("'", "`"), list.getValue4().toUpperCase().replace("'", "`"));
                try {
                    writer.write(SQL);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateResultUsingLgasInState() {
        List<Tuple> readList = readFromFile();

        List<Quintet<String, String, String, String, String>> quintetList = readList
                .stream()
                .map(lga ->
                        getTopThreeNearestLgas((Quartet<String, String, Double, Double>) lga, readList.stream().filter(l->l.getValue(1).equals(((Quartet<?, ?, ?, ?>) lga).getValue1())).collect(Collectors.toList()))
                ).collect(Collectors.toList());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("nearest_lgas_in_state.sql"))) {
            quintetList.forEach(list-> {
                String SQL = String.format
                        ("INSERT INTO nearest_lga_lookup (lga_name, first_nearest_lga, second_nearest_lga, third_nearest_lga, " + "state_name) " +
                                "VALUES (\'%s\', \'%s\', \'%s\', \'%s\', \'%s\');\n", list.getValue0().toUpperCase().replace("'", "`"), list.getValue1().toUpperCase().replace("'", "`"), list.getValue2().toUpperCase().replace("'", "`"), list.getValue3().toUpperCase().replace("'", "`"), list.getValue4().toUpperCase().replace("'", "`"));
                try {
                    writer.write(SQL);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatDate() {
        LocalDate localDate = LocalDate.now().minusDays(1);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

        return dateTimeFormatter.format(localDate);
    }

    public static void main(String[] args) {
        check();
        generateResultUsingTheTotalLgas();
        generateResultUsingLgasInState();
    }
}
