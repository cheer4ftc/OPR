package com.wrgardnersoft.opr;

import com.wrgardnersoft.opr.models.FtcScoringFileReader;
import com.wrgardnersoft.opr.models.OPR;

import static java.lang.System.exit;

public class Main {

    static int MAX_NUM_TEAMS = 256; // lazy for demo code

    public static void main(String[] args) {

        double mmse = 1.0;
        int teamsPerAlliance = 2; // 3 for FRC

        int[] team;
        double[] opr, oprAuto, oprTele, oprEndg, oprPenFor, oprPenAgainst;
        int numScores;

        String inputFilename = "assets/testfiles/1617velv-cmpmo-d1-MatchResultsDetails.html";

        // read commandline arguments
        if (args.length > 0) {
            int argind = 0;
            while (argind < args.length) {
                if (args[argind].equals("h")) {
                    showHelp();
                } else if (args[argind].equals("m")) {
                    argind++;
                    mmse = Double.parseDouble(args[argind++]);
                } else if (args[argind].equals("f")) {
                    argind++;
                    inputFilename = args[argind++];
                } else {
                    System.err.println("Error: unknown or malformatted command line argument");
                    showHelp();
                }
            }
        }

        // read input file
        FtcScoringFileReader.FtcEventResult event = FtcScoringFileReader.readMatchDetails(inputFilename, true);

        team = OPR.getTeamList(teamsPerAlliance, event.teamPlaying);

        opr = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.TOTAL.ordinal()]);
        if (opr == null) {
            System.err.println("Error: OPR could not be computed for this file");
            exit(-1);
        }

        // result OK, compute component OPRs too.
        oprAuto = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.AUTO.ordinal()]);
        oprTele = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.TELE.ordinal()]);
        oprEndg = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.ENDG.ordinal()]);
        oprPenFor = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.PENFOR.ordinal()]);
        oprPenAgainst = OPR.computeMMSE(mmse, team, teamsPerAlliance, event.teamPlaying, event.score[FtcScoringFileReader.SCORE_TYPE.PENAGAINST.ordinal()]);

        // output results
        for (int t = 0; t < team.length; t++) {
            System.out.printf("%6d", team[t]);
            System.out.printf(", %7.2f", opr[t]);
            System.out.printf(", %7.2f", oprAuto[t]);
            System.out.printf(", %7.2f", oprTele[t]);
            System.out.printf(", %7.2f", oprEndg[t]);
            System.out.printf(", %7.2f", oprPenFor[t]);
            System.out.printf(", %7.2f", oprPenAgainst[t]);
            System.out.println();
        }
    }

    static void showHelp() {
        System.err.println("Compute OPRs from FTC Scoring file MatchResultsDetails.html file");
        System.err.println("  h          : show this help message");
        System.err.println("  f filename : filename of source html file");
        System.err.println("  m mmse     : mmse parameter for OPR calculation (0=regular OPR, recommend 1-3");
        System.exit(-1);
    }
}
