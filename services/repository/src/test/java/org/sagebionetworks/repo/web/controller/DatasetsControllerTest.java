/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Unit tests for the Dataset CRUD operations exposed by the DatasetController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * datasets.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class DatasetsControllerTest {

	private static final Logger log = Logger
			.getLogger(DatasetsControllerTest.class.getName());
	private Helpers helper = new Helpers();
	private DispatcherServlet servlet;

	private String sampleDatasetNames[] = { "DeLiver", "MouseCross",
			"Harvard Brain", "Glioblastoma TCGA",
			"Mouse Model of Diet-Induced Atherosclerosis",
			"TCGA Curation Package",
			"Mouse Model of Sexually Dimorphic Atherosclerotic Traits",
			"Hepatocellular Carcinoma HongKong", "Human Liver Cohort",
			"METABRIC Breast Cancer", "Harvard Brain Tissue Resource Center",
			"Pediatric AML TARGET", "Flint HS Mice", };

	private DateTime sampleDates[] = { new DateTime("2010-01-01"),
			new DateTime("2000-06-06"), new DateTime("2011-01-15"),
			new DateTime("2011-01-14"), new DateTime("2000-08-08"),
			new DateTime("1999-02-02"), new DateTime("2003-05-05"),
			new DateTime("2003-05-06"), new DateTime("2007-07-30"),
			new DateTime("2007-07-07"), new DateTime("2007-07-22"),
			new DateTime("2007-07-22"), new DateTime("2007-07-18"), };

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsPageOneOfTwo() throws Exception {
		int totalNumDatasets = sampleDatasetNames.length;

		// Load up a few datasets
		for (int i = 0; i < totalNumDatasets; i++) {
			helper.testCreateJsonEntity("/dataset", "{\"name\":\""
					+ sampleDatasetNames[i] + "\"}");
		}

		JSONObject results = helper.testGetJsonEntities("/dataset", null, null,
				null, null);
		assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
		assertEquals(10, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertEquals(
				"/dataset?offset=11&limit=10",
				results.getJSONObject("paging").getString(
						PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedDatasetsProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsPageTwoOfTwo() throws Exception {
		int totalNumDatasets = sampleDatasetNames.length;

		// Load up a few datasets
		for (int i = 0; i < totalNumDatasets; i++) {
			helper.testCreateJsonEntity("/dataset", "{\"name\":\""
					+ sampleDatasetNames[i] + "\"}");
		}

		JSONObject results = helper.testGetJsonEntities("/dataset", 11, 10,
				null, null);
		assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
		assertEquals(totalNumDatasets - 10, results.getJSONArray("results")
				.length());
		assertEquals(
				"/dataset?offset=1&limit=10",
				results.getJSONObject("paging").getString(
						PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedDatasetsProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsSortByPrimaryFieldAscending() throws Exception {
		int totalNumDatasets = sampleDatasetNames.length;

		// Load up a few datasets
		for (int i = 0; i < totalNumDatasets; i++) {
			helper.testCreateJsonEntity("/dataset", "{\"name\":\""
					+ sampleDatasetNames[i] + "\"}");
		}

		List<String> sortedDatasetNames = Arrays.asList(sampleDatasetNames);
		Collections.sort(sortedDatasetNames);

		JSONObject results = helper.testGetJsonEntities("/dataset", null, 5,
				"name", true);
		assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
		assertEquals(5, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertEquals(
				"/dataset?offset=6&limit=5&sort=name&ascending=true",
				results.getJSONObject("paging").getString(
						PaginatedResults.NEXT_PAGE_FIELD));
		for (int i = 0; i < 5; i++) {
			assertEquals(
					sortedDatasetNames.get(i),
					results.getJSONArray("results").getJSONObject(i)
							.getString("name"));
		}

		assertExpectedDatasetsProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsSortByPrimaryFieldDescending() throws Exception {
		int totalNumDatasets = sampleDatasetNames.length;

		// Load up a few datasets
		for (int i = 0; i < totalNumDatasets; i++) {
			helper.testCreateJsonEntity("/dataset", "{\"name\":\""
					+ sampleDatasetNames[i] + "\"}");
		}

		List<String> sortedDatasetNames = Arrays.asList(sampleDatasetNames);
		Collections.sort(sortedDatasetNames);

		JSONObject results = helper.testGetJsonEntities("/dataset", null, 5,
				"name", false);
		assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
		assertEquals(5, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertEquals(
				"/dataset?offset=6&limit=5&sort=name&ascending=false",
				results.getJSONObject("paging").getString(
						PaginatedResults.NEXT_PAGE_FIELD));
		for (int i = 0; i < 5; i++) {
			assertEquals(
					sortedDatasetNames.get(sortedDatasetNames.size() - 1 - i),
					results.getJSONArray("results").getJSONObject(i)
							.getString("name"));
		}

		assertExpectedDatasetsProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetDatasetsSortByStringAnnotationAscending()
			throws Exception {
		int totalNumDatasets = sampleDatasetNames.length;

		// Load up a few datasets
		for (int i = 0; i < totalNumDatasets; i++) {
			JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
					"{\"name\":\"" + sampleDatasetNames[i] + "\"}");

			// Get our empty annotations container
			JSONObject annotations = helper.testGetJsonEntity(newDataset
					.getString("annotations"));

			// Put our annotations
			String secondaryName[] = { sampleDatasetNames[i] };
			JSONObject stringAnnotations = annotations
					.getJSONObject("stringAnnotations");
			stringAnnotations.put("secondaryName", secondaryName);
			helper.testUpdateJsonEntity(annotations);

			// Now check that we correctly persisted them for real
			JSONObject storedAnnotations = helper.testGetJsonEntity(newDataset
					.getString("annotations"));
			helper.assertJSONArrayEquals(secondaryName,
					storedAnnotations.getJSONObject("stringAnnotations")
							.getJSONArray("secondaryName"));
		}

		List<String> sortedDatasetNames = Arrays.asList(sampleDatasetNames);
		Collections.sort(sortedDatasetNames);

		JSONObject results = helper.testGetJsonEntities("/dataset", null, 5,
				"secondaryName", true);
		assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
		assertEquals(5, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertEquals(
				"/dataset?offset=6&limit=5&sort=secondaryName&ascending=true",
				results.getJSONObject("paging").getString(
						PaginatedResults.NEXT_PAGE_FIELD));
		for (int i = 0; i < 5; i++) {

			// Since I created these datasets with primaryField("name") ==
			// stringAnnotation("name")
			// we can use the primary field as a proxy to know that it was
			// sorted correctly on the annotation field
			assertEquals(
					sortedDatasetNames.get(i),
					results.getJSONArray("results").getJSONObject(i)
							.getString("name"));
		}

		assertExpectedDatasetsProperties(results.getJSONArray("results"));
	}

	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	// * .
	// *
	// * @throws Exception
	// */
	// @SuppressWarnings("unchecked")
	// @Test
	// @Ignore
	// public void testGetDatasetsSortByDateAnnotationAscending() throws
	// Exception {
	// int totalNumDatasets = sampleDatasetNames.length;
	// Map<String, DateTime> testCases = new HashMap<String, DateTime>();
	//
	// // Load up a few datasets
	// for (int i = 0; i < totalNumDatasets; i++) {
	// JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"" + sampleDatasetNames[i] + "\"}");
	//
	// // Get our empty annotations container
	// JSONObject annotations = helper.testGetJsonEntity(newDataset
	// .getString("annotations"));
	//
	// // Put our date annotations
	// Long curationEvents[] = { sampleDates[i].getMillis() };
	// JSONObject dateAnnotations = annotations
	// .getJSONObject("dateAnnotations");
	// dateAnnotations.put("curationEvents", curationEvents);
	// JSONObject results = helper.testUpdateJsonEntity(annotations);
	//
	// // Now check that we correctly persisted them for real
	// JSONObject storedAnnotations = helper.testGetJsonEntity(newDataset
	// .getString("annotations"));
	// helper.assertJSONArrayEquals(curationEvents,
	// storedAnnotations.getJSONObject(
	// "dateAnnotations").getJSONArray("curationEvents"));
	//
	// testCases.put(newDataset.getString("id"), sampleDates[i]);
	//
	// }
	//
	// List<DateTime> sortedDates = Arrays.asList(sampleDates);
	// Collections.sort(sortedDates);
	//
	// JSONObject results = helper.testGetJsonEntities("/dataset", null, 5,
	// "curationEvents", true);
	// assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
	// assertEquals(5, results.getJSONArray("results").length());
	// assertFalse(results.getJSONObject("paging").has(
	// PaginatedResults.PREVIOUS_PAGE_FIELD));
	// assertEquals("/dataset?offset=6&limit=5&sort=curationEvents&ascending=true",
	// results.getJSONObject("paging").getString(
	// PaginatedResults.NEXT_PAGE_FIELD));
	// for (int i = 0; i < 5; i++) {
	// DateTime expectedDate = sortedDates.get(i);
	// DateTime actualDate = testCases.get(results.getJSONArray(
	// "results").getJSONObject(i).getString("id"));
	// assertEquals(expectedDate, actualDate);
	// }
	//
	// assertExpectedDatasetsProperties(results.getJSONArray("results"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	// * .
	// *
	// * @throws Exception
	// */
	// @SuppressWarnings("unchecked")
	// @Test
	// @Ignore
	// public void testGetDatasetsSortByDateAnnotationDescending() throws
	// Exception {
	// int totalNumDatasets = sampleDatasetNames.length;
	//
	// // Load up a few datasets
	// for (int i = 0; i < totalNumDatasets; i++) {
	// JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"" + sampleDatasetNames[i] + "\"}");
	//
	// // Get our empty annotations container
	// JSONObject annotations = helper.testGetJsonEntity(newDataset
	// .getString("annotations"));
	//
	// Long curationEvents[] = { sampleDates[i].getMillis() };
	// JSONObject dateAnnotations = annotations
	// .getJSONObject("dateAnnotations");
	// dateAnnotations.put("curationEvents", curationEvents);
	// JSONObject results = helper.testUpdateJsonEntity(annotations);
	// }
	//
	// List<DateTime> sortedDates = Arrays.asList(sampleDates);
	// Collections.sort(sortedDates);
	//

	// TODO this needs more tweaks similar to those in the test above

	// JSONObject results = helper.testGetJsonEntities("/dataset", null, 5,
	// "name", false);
	// assertEquals(totalNumDatasets, results.getInt("totalNumberOfResults"));
	// assertEquals(5, results.getJSONArray("results").length());
	// assertFalse(results.getJSONObject("paging").has(
	// PaginatedResults.PREVIOUS_PAGE_FIELD));
	// assertEquals("/dataset?offset=6&limit=5&sort=name&ascending=false",
	// results.getJSONObject("paging").getString(
	// PaginatedResults.NEXT_PAGE_FIELD));
	// for (int i = 0; i < 5; i++) {
	// assertEquals(sortedDates.get(sortedDates.size() - 1
	// - i), results.getJSONArray("results").getJSONObject(i)
	// .getString("name"));
	// }
	//
	// assertExpectedDatasetsProperties(results.getJSONArray("results"));
	// }

	// TODO sort on a date property
	// TODO sort on a numeric property
	// TODO sort on an annotation property

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsBadLimit() throws Exception {

		JSONObject error = helper.testGetJsonEntitiesShouldFail("/dataset", 1,
				0, null, null, HttpStatus.BAD_REQUEST);
		assertEquals("pagination limit must be 1 or greater",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDatasetsBadOffset() throws Exception {
		JSONObject error = helper.testGetJsonEntitiesShouldFail("/dataset", -5,
				10, null, null, HttpStatus.BAD_REQUEST);
		assertEquals("pagination offset must be 1 or greater",
				error.getString("reason"));
	}

	/*****************************************************************************************************
	 * Datasets-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 * 
	 */
	public static void assertExpectedDatasetsProperties(JSONArray results)
			throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject dataset = results.getJSONObject(i);
			DatasetControllerTest.assertExpectedDatasetProperties(dataset);
		}
	}

}