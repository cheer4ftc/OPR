package com.wrgardnersoft.opr.models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;

public class FtcScoringFileReader {

    public enum SCORE_TYPE {TOTAL, AUTO, TELE, ENDG, PENFOR, PENAGAINST}

    private static int MAX_MATCHES = 256;
    private static int NUM_ALLIANCES = 2;
    private static int MAX_TEAMS_PER_ALLIANCE = 3;

    public static class FtcEventResult {
        String[] matchName;
        public int[][][] teamPlaying;
        public int[][][] score;

        FtcEventResult(int numMatches, int numScores, int teamsPerAlliance, String[] mn, int[][][] tp, int[][][] sc) {
            matchName = new String[numMatches];
            teamPlaying = new int[numMatches][NUM_ALLIANCES][teamsPerAlliance];
            score = new int[numScores][numMatches][NUM_ALLIANCES];

            for (int i = 0; i < numMatches; i++) {
                matchName[i] = mn[i];

                for (int j = 0; j < NUM_ALLIANCES; j++) {
                    for (int k = 0; k < teamsPerAlliance; k++) {
                        teamPlaying[i][j][k] = tp[i][j][k];
                    }
                }

                for (int j = 0; j < numScores; j++) {
                    for (int k = 0; k < NUM_ALLIANCES; k++) {
                        score[j][i][k] = sc[j][i][k];
                    }
                }
            }
        }
    }


    /**
     * Reads MatchResultsDetails html output from FTC scoring system and returns teams and scores
     *
     * @param filename input name of file to read
     * @param qualOnly input boolean. If true, only return qual match info.
     * @return FtcEventResult object with arrays of teamsPlaying and scores
     */
    static public FtcEventResult readMatchDetails(String filename, boolean qualOnly) {
        int[][][] teamPlaying = new int[MAX_MATCHES][NUM_ALLIANCES][MAX_TEAMS_PER_ALLIANCE];
        int[][][] score = new int[SCORE_TYPE.values().length][MAX_MATCHES][NUM_ALLIANCES];
        String[] matchName = new String[MAX_MATCHES];

        int numMatches = 0;

        File inFile = new File(filename);
        Document document=null;
        try {
            document = Jsoup.parse(inFile, "UTF-8");
        } catch (Exception e) {
            System.err.println("Error: can't open filename:" + filename);
            System.exit(-1);
        }

        boolean tableOK = false;
        Element table;
        Elements rows=null;
        int rowIndex = 0;
        try {
            int tableIndex = 0;

            while (!tableOK) {
                table = document.select("table").get(tableIndex);
                rows = table.select("tr");
                for (rowIndex = 0; (rowIndex < rows.size()) && ((rows.get(rowIndex).select("th").size() < 5) ||
                        (!rows.get(rowIndex).select("th").get(4).text().contentEquals("Red Scores"))); rowIndex++) {
                }
                try {
                    if (rows.get(rowIndex).select("th").get(4).text().contentEquals("Red Scores")) {
                        rowIndex++;
                    }
                } catch (Exception e1) {
                }

                if (rowIndex < rows.size()) {
                    tableOK = true;
                } else {
                    tableIndex++;
                    table = document.select("table").get(tableIndex);
                    rows = table.select("tr");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: problem reading filename:" + filename);
            System.exit(-1);
        }

        table = rows.get(rowIndex).select("th").get(0);
        while (!((table == null) || (table.tagName().equalsIgnoreCase("table")))) {
            table = table.parent();
        }
        rows = table.select("tr");

        for (int j = 2; j < rows.size(); j++) { //first row is the col names so skip it.
            Element row = rows.get(j);
            Elements cols = row.select("td");

            if (cols.size() == 16) {// teams are packed into one element

                if (cols.get(0).text().length()>0) { // test for blank row, per 2017 WSR :(

                    String matchTitle = cols.get(0).text();
                    if ((!qualOnly) || (matchTitle.substring(0,2).equals("Q-"))) {
                        String redTeamString = cols.get(2).text();
                        String blueTeamString = cols.get(3).text();
                        String redTeamResult[] = redTeamString.replaceAll("\\*", "").split("\\s+");
                        String blueTeamResult[] = blueTeamString.replaceAll("\\*", "").split("\\s+");

                        int k = 0;
                        for (String team : redTeamResult) {
                            teamPlaying[numMatches][0][k] = Integer.parseInt(redTeamResult[k]);
                            k++;
                        }
                        while (k<MAX_TEAMS_PER_ALLIANCE) {
                            teamPlaying[numMatches][0][k++]=0;
                        }
                        k = 0;
                        for (String team : blueTeamResult) {
                            teamPlaying[numMatches][1][k] = Integer.parseInt(blueTeamResult[k]);
                            k++;
                        }
                        while (k<MAX_TEAMS_PER_ALLIANCE) {
                            teamPlaying[numMatches][1][k++]=0;
                        }
                        score[SCORE_TYPE.TOTAL.ordinal()][numMatches][0] = Integer.parseInt(cols.get(4).text());
                        score[SCORE_TYPE.AUTO.ordinal()][numMatches][0] = Integer.parseInt(cols.get(5).text());
                        score[SCORE_TYPE.TELE.ordinal()][numMatches][0] = Integer.parseInt(cols.get(7).text());
                        score[SCORE_TYPE.ENDG.ordinal()][numMatches][0] = Integer.parseInt(cols.get(8).text());
                        score[SCORE_TYPE.PENFOR.ordinal()][numMatches][0] = Integer.parseInt(cols.get(9).text());
                        score[SCORE_TYPE.PENAGAINST.ordinal()][numMatches][1] = Integer.parseInt(cols.get(9).text());

                        score[SCORE_TYPE.TOTAL.ordinal()][numMatches][1] = Integer.parseInt(cols.get(10).text());
                        score[SCORE_TYPE.AUTO.ordinal()][numMatches][1] = Integer.parseInt(cols.get(11).text());
                        score[SCORE_TYPE.TELE.ordinal()][numMatches][1] = Integer.parseInt(cols.get(13).text());
                        score[SCORE_TYPE.ENDG.ordinal()][numMatches][1] = Integer.parseInt(cols.get(14).text());
                        score[SCORE_TYPE.PENFOR.ordinal()][numMatches][1] = Integer.parseInt(cols.get(15).text());
                        score[SCORE_TYPE.PENAGAINST.ordinal()][numMatches][0] = Integer.parseInt(cols.get(15).text());

                        numMatches++;
                    }
                }
            } else {
                System.err.println("Error: problem reading file, not 16 columns in the table?");
                System.exit(-1);
            }
        }

        if (qualOnly) { // return only 2 team alliances
            return new FtcEventResult(numMatches, SCORE_TYPE.values().length, 2, matchName, teamPlaying, score);
        } else {
            return new FtcEventResult(numMatches, SCORE_TYPE.values().length, 3, matchName, teamPlaying, score);
        }
    }

}
