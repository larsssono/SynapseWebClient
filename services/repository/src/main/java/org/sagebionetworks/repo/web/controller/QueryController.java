package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.QueryStatement;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author deflaux
 * 
 */
@Controller
public class QueryController extends BaseController {

	private EntitiesAccessor<Dataset> datasetAccessor;
	private DatasetDAO dao;
	
	// Use a static instance of this per
	// http://wiki.fasterxml.com/JacksonBestPracticesPerformance
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();

	
	QueryController() {
		datasetAccessor = new AnnotatableEntitiesAccessorImpl<Dataset>();
	}

	private void checkAuthorization(String userId) {
		BaseDAO<Dataset> dao = DAO_FACTORY.getDatasetDAO(userId);
		setDao(dao);
	}

	/**
	 * @param dao
	 */
	public void setDao(BaseDAO<Dataset> dao) {
		this.dao = (DatasetDAO) dao;
		datasetAccessor.setDao(dao);
	}

	/**
	 * @param userId
	 * @param query
	 * @param request
	 * @return paginated results
	 * @throws DatastoreException
	 * @throws ParseException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@SuppressWarnings("unchecked")
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.QUERY, method = RequestMethod.GET)
	public @ResponseBody
	QueryResults query(
			@RequestParam(value = ServiceConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.QUERY_PARAM, required = true) String query,
			HttpServletRequest request) throws DatastoreException,
			ParseException, NotFoundException, UnauthorizedException {

		checkAuthorization(userId);

		/**
		 * Parse and validate the query
		 */
		QueryStatement stmt = new QueryStatement(query);
		if (null == stmt.getTableName()
				|| !stmt.getTableName().equals("dataset")) {
			throw new ParseException(
					"Queries are only supported for datasets at this time");
		}

		/**
		 * Perform the query
		 * 
		 * TODO talk to Bruce to see if he would prefer that this stuff is
		 * transformed in to a query string that JDO understands
		 */
		List<Dataset> datasets;
		if (null != stmt.getWhereField() && null != stmt.getWhereValue()) {
			// TODO only == is supported for InRangeHaving
			datasets = datasetAccessor.getInRangeHaving(stmt.getOffset(), stmt
					.getLimit(), stmt.getWhereField(), stmt.getWhereValue());
		} else if (null != stmt.getSortField()) {
			datasets = datasetAccessor.getInRangeSortedBy(stmt.getOffset(),
					stmt.getLimit(), stmt.getSortField(), stmt
							.getSortAcending());
		} else {
			datasets = datasetAccessor.getInRange(stmt.getOffset(), stmt
					.getLimit());
		}

		/**
		 * Get the total number of results for this query
		 * 
		 * TODO we don't have this for queries with a WHERE clause
		 */
		Integer totalNumberOfResults = dao.getCount();

		/**
		 * Format the query result
		 */
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for (Dataset dataset : datasets) {

			Map<String, Object> result = OBJECT_MAPPER.convertValue(dataset,
					Map.class);
			// Get rid of fields for REST api
			result.remove("uri");
			result.remove("etag");
			result.remove("annotations");
			result.remove("layer");

			Annotations annotations = dao
					.getAnnotations(dataset.getId());
			result.putAll(annotations.getStringAnnotations());
			result.putAll(annotations.getDoubleAnnotations());
			result.putAll(annotations.getLongAnnotations());
			result.putAll(annotations.getDateAnnotations());
			results.add(result);

			// TODO filter out un-requested fields when we support more than
			// SELECT *

			// TODO get rid of etag and uri
		}

		return new QueryResults(results, totalNumberOfResults);
	}
}