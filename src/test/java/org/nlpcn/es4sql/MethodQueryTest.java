package org.nlpcn.es4sql;

import java.io.IOException;

import org.elasticsearch.action.search.SearchResponse;
import org.junit.Test;
import org.nlpcn.es4sql.exception.SqlParseException;

public class MethodQueryTest {
	private SearchDao searchDao = new SearchDao("localhost", 9300);

	/**
	 * query 搜索就是　，　lucene 原生的搜素方式 注意这个例子中ｖａｌｕｅ可以随便命名 "query" :
	 * {query_string" : {"query" : "address:880 Holmes Lane"}
	 * 
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void queryTest() throws IOException, SqlParseException {
		SearchResponse select = searchDao.select("select address from bank where q= query('880 Holmes Lane') limit 3");
		System.out.println(select);
	}

	/**
	 * matchQuery 是利用分词结果进行单个字段的搜索． "query" : { "match" : { "address" :
	 * {"query":"880 Holmes Lane", "type" : "boolean" } } }
	 * 
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void matchQueryTest() throws IOException, SqlParseException {
		SearchResponse select = searchDao.select("select address from bank where address= matchQuery('880 Holmes Lane') limit 3");
		System.out.println(select);
	}

	/**
	 * matchQuery 是利用分词结果进行单个字段的搜索． "query" : { "bool" : { "must" : { "bool" : {
	 * "should" : [ { "constant_score" : { "query" : { "match" : { "address" : {
	 * "query" : "Lane", "type" : "boolean" } } }, "boost" : 100.0 } }, {
	 * "constant_score" : { "query" : { "match" : { "address" : { "query" :
	 * "Street", "type" : "boolean" } } }, "boost" : 0.5 } } ] } } } }
	 * 
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void scoreQueryTest() throws IOException, SqlParseException {
		SearchResponse select = searchDao
				.select("select address from bank where address= scoreQuery(matchQuery('Lane'),100) or address= scoreQuery(matchQuery('Street'),0.5)  order by _score desc limit 3");
		System.out.println(select);
	}

	/**
	 * wildcardQuery 是用通配符的方式查找某个term 　比如例子中 l*e means leae ltae ....
	 * "wildcard": { "address" : { "wildcard" : "l*e" } }
	 * 
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void wildcardQueryTest() throws IOException, SqlParseException {
		SearchResponse select = searchDao.select("select address from bank where address= wildcardQuery('l*e')  order by _score desc limit 3");
		System.out.println(select);
	}
	
	
	/**
	 * matchPhraseQueryTest 短语查询完全匹配．
	 * "address" : {
                "query" : "671 Bristol Street",
                "type" : "phrase"
              }
	 * 
	 * @throws IOException
	 * @throws SqlParseException
	 */
	@Test
	public void matchPhraseQueryTest() throws IOException, SqlParseException {
		SearchResponse select = searchDao.select("select address from bank where address= matchPhraseQuery('671 Bristol Street')  order by _score desc limit 3");
		System.out.println(select);
	}
}