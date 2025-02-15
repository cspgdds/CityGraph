package main;

import main.OpenRouteAPI.MatrixBody;
import main.OpenRouteAPI.MatrixResponse;
import main.OpenRouteAPI.OpenRouteApiWrapper;
import main.SimpleMaps.SimpleMapsWrapper;
import org.apache.commons.cli.*;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.WeightedMultigraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Main {

	private static OpenRouteApiWrapper openRouteWrapper;
	private static SimpleMapsWrapper mapsWrapper;
	private static String API_KEY;
	private static long START = 0;

	public static void main(String[] args) {
		START = System.currentTimeMillis();
		Options options = createCommandLineOptions();
		parseCommands(options, args);
	}

	private static Options createCommandLineOptions() {
		Options options = new Options();
		options.addOption("h", "help", false, "Shows help");
		options.addOption("a", "all", true, "Exports Locations.csv, Routes.csv, and StraightLine.csv with Walk, Drive and StraightLine distance metrics" +
				" args: API_KEY COUNTRY_CODE POPULATION_LIMIT CITIES_LIMIT");
		options.addOption("al", "alllocations", true, "Exports Locations.csv, Routes.csv and StraightLine.csv with Walk, Drive and StraightLine distance metrics" +
				"using only extra cities");
		options.addOption("d", "drive", true, "Exports Locations.csv and Routes.csv with Drive distance metric" +
				" args: API_KEY COUNTRY_CODE POPULATION_LIMIT CITIES_LIMIT");
		options.addOption("w", "walk", true, "Exports Locations.csv and Routes.csv with Walk distance metric" +
				" args: API_KEY COUNTRY_CODE POPULATION_LIMIT CITIES_LIMIT");
		options.addOption("s", "straight", true, "Exports StraightLine.csv with StraightLine metric" +
				" args: API_KEY COUNTRY_CODE POPULATION_LIMIT CITIES_LIMIT");
		return options;

	}

	private static void parseCommands(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("ERROR: Unable to parse command-line arguments "
					+ Arrays.toString(args) + " due to: " + e);
		}

		if (cmd != null) {

			if (cmd.hasOption("a")) {
				System.out.println("This will export Locations.csv, Routes.csv, and StraightLine.csv");
				System.out.println("With Drive, Walk and StraightLine distance metrics");
				API_KEY = args[1];
				String countryCode = args[2];
				int population = Integer.parseInt(args[3]);
				int cityLimit = Integer.parseInt(args[4]);
				openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
				List<City> cityList = createCityList(countryCode, population, cityLimit);
				exportLocationCsv(countryCode, cityList);
				driveWalkAndStraightLineMetric(countryCode, cityList);
			} else if (cmd.hasOption("al")) {
				System.out.println("This will export Locations.csv, Routes.csv, and StraightLine.csv");
				System.out.println("With Drive, Walk and StraightLine distance metrics");
				System.out.println("Only using cities.csv and NOT worldcities.csv");
				API_KEY = args[1];
				String countryCode = args[2];
				int population = Integer.parseInt(args[3]);
				int cityLimit = Integer.parseInt(args[4]);
				openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
				List<City> cityList = CSVHelper.cityListByCitiesInput(countryCode);
				if(cityList == null){
					System.out.println("The cities.csv could not be parsed, check the names of the columns");
					System.exit(9);
				}
				CSVHelper.removeCitiesBasedOnFilter(cityList, new CSVCityLimit(population, cityLimit));
				exportLocationCsv(countryCode, cityList);
				driveWalkAndStraightLineMetric(countryCode, cityList);
			} else if (cmd.hasOption("d")) {
				System.out.println("This will export Locations.csv and Routes.csv");
				System.out.println("With Drive distance metric");
				API_KEY = args[1];
				String countryCode = args[2];
				int population = Integer.parseInt(args[3]);
				int cityLimit = Integer.parseInt(args[4]);
				openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
				List<City> cityList = createCityList(countryCode, population, cityLimit);
				exportLocationCsv(countryCode, cityList);
				driveMetric(countryCode, cityList);
			} else if (cmd.hasOption("w")) {
				System.out.println("This will export Locations.csv and Routes.csv");
				System.out.println("With Walk distance metric");
				API_KEY = args[1];
				String countryCode = args[2];
				int population = Integer.parseInt(args[3]);
				int cityLimit = Integer.parseInt(args[4]);
				openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
				List<City> cityList = createCityList(countryCode, population, cityLimit);
				exportLocationCsv(countryCode, cityList);
				walkMetric(countryCode, cityList);
			} else if (cmd.hasOption("s")) {
				System.out.println("This will StraightLine.csv");
				System.out.println("With StraightLine distance metric");
				API_KEY = args[1];
				String countryCode = args[2];
				int population = Integer.parseInt(args[3]);
				int cityLimit = Integer.parseInt(args[4]);
				openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
				List<City> cityList = createCityList(countryCode, population, cityLimit);
				exportLocationCsv(countryCode, cityList);
				straightMetric(countryCode, cityList);
				System.out.println("Completed export with Straight distance metric");
			} else if (cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Command Line Parameters", options);
			}
		}
	}

	/*Use the URL of simpleMaps and country code to download the country cities

	private static List<City> downloadAndCreateCityList(String countryCode, int population, int cityLimit) {
		openRouteWrapper = new OpenRouteApiWrapper(API_KEY);
		mapsWrapper = new SimpleMapsWrapper();
		mapsWrapper.downloadCountryCSVByCode(countryCode);
		List<City> cityList = CSVHelper.cityListByCountryName(countryCode, new CSVCityLimit(population, cityLimit));
		return cityList;
	}
	*/

	//Use the offline download of the cities to create City List
	private static List<City> createCityList(String countryCode, int population, int cityLimit) {
		return CSVHelper.cityListByCountryNameOffline(countryCode, new CSVCityLimit(population, cityLimit));
	}

	private static void exportLocationCsv(String countryCode, List<City> cityList) {
		CSVHelper.exportLocationGraphToCustomCSVFormat(cityList, countryCode + "_locations");
	}

	private static void driveWalkAndStraightLineMetric(String countryCode, List<City> cityList) {

		straightMetric(countryCode, cityList);
		MatrixResponse driveMatrixResponse = requestDriveMatrixFromAPI(cityList);
		MatrixResponse walkMatrixResponse = requestWalkMatrixFromAPI(cityList);

		if (driveMatrixResponse != null && walkMatrixResponse != null) {

			WeightedMultigraph drivingGraph = GraphHelper.createWeightedGraphDrivingAndWalkDistance(cityList, driveMatrixResponse.getDistances(), walkMatrixResponse.getDistances());
			CSVHelper.exportGraphToCustomCSVFormat(drivingGraph, countryCode + "_routes");
			System.out.println("Completed export with Driving, Walking and StraightLine distance metrics");
			System.exit(1);
		}
	}

	private static void driveMetric(String countryCode, List<City> cityList) {

		MatrixResponse driveMatrixResponse = requestDriveMatrixFromAPI(cityList);

		if (driveMatrixResponse != null) {

			WeightedMultigraph drivingGraph = GraphHelper.createWeightedGraphDrivingDistance(cityList, driveMatrixResponse.getDistances());
			CSVHelper.exportGraphToCustomCSVFormat(drivingGraph, countryCode + "_routes");
			System.out.println("Completed export with Driving distance metric");
			System.exit(2);
		}
	}

	private static void walkMetric(String countryCode, List<City> cityList) {

		MatrixResponse walkMatrixResponse = requestWalkMatrixFromAPI(cityList);

		if (walkMatrixResponse != null) {

			WeightedMultigraph drivingGraph = GraphHelper.createWeightedGraphWalkingDistance(cityList, walkMatrixResponse.getDistances());
			CSVHelper.exportGraphToCustomCSVFormat(drivingGraph, countryCode + "_routes");
			System.out.println("Completed export with Walking distance metric");
			System.exit(2);
		}
	}

	private static void straightMetric(String countryCode, List<City> cityList) {
		//Create a simple weighted graph with data (StraightLine Distance)
		//Export graph file to Resources/test.csv*/
		SimpleWeightedGraph strGraph = GraphHelper.createWeightedGraphStraightLineDistance(cityList);
		CSVHelper.exportGraphToDefaultCSVFormat(strGraph, countryCode + "_straight");
	}

	private static MatrixResponse requestDriveMatrixFromAPI(List<City> cityList) {

		List<List<Double>> cityGPSList = createGPSLocationsList(cityList);

		//Create the body for the POST request (GPS list, Distance, Units)
		MatrixBody matrixBody = new MatrixBody(cityGPSList, MatrixBody.DISTANCE, MatrixBody.KM);
		//Send POST request with body
		MatrixResponse driveMatrixResponse = openRouteWrapper.getMatrixDistanceByCar(matrixBody);
		System.out.println("Drive distances API request completed: " + ((System.currentTimeMillis() - START) / 1000) + " seconds");
		return driveMatrixResponse;
	}

	private static MatrixResponse requestWalkMatrixFromAPI(List<City> cityList) {

		List<List<Double>> cityGPSList = createGPSLocationsList(cityList);

		//Create the body for the POST request (GPS list, Distance, Units)
		MatrixBody matrixBody = new MatrixBody(cityGPSList, MatrixBody.DISTANCE, MatrixBody.KM);
		//Send POST request with body
		MatrixResponse footMatrixResponse = openRouteWrapper.getMatrixDistanceByFoot(matrixBody);
		System.out.println("Foot distances API request completed: " + ((System.currentTimeMillis() - START) / 1000) + " seconds");
		return footMatrixResponse;
	}

	private static List<List<Double>> createGPSLocationsList(List<City> cityList) {

		//Create a list of list of GPS to send as parameter to matrix endpoint
		List<List<Double>> cityGPSList = new ArrayList<>();
		for (City c : cityList) {
			List<Double> list = new ArrayList<>();
			list.add(c.getLon());
			list.add(c.getLat());
			cityGPSList.add(list);
		}
		return cityGPSList;
	}
}
