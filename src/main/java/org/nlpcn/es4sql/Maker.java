package org.nlpcn.es4sql;

import java.util.Set;

import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.nlpcn.es4sql.domain.Condition;
import org.nlpcn.es4sql.domain.Condition.OPEAR;
import org.nlpcn.es4sql.domain.Paramer;
import org.nlpcn.es4sql.exception.SqlParseException;

import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;

public abstract class Maker {

	private static final Set<OPEAR> NOT_OPEAR_SET = Sets.newHashSet(OPEAR.N, OPEAR.NIN, OPEAR.ISN);

	private boolean isQuery = false;

	protected Maker(Boolean isQuery) {
		this.isQuery = isQuery;
	}

	/**
	 * 构建过滤条件
	 * 
	 * @param boolFilter
	 * @param expr
	 * @param expr
	 * @return
	 * @throws SqlParseException
	 */
	protected ToXContent make(Condition cond) throws SqlParseException {

		String name = cond.getName();
		Object value = cond.getValue();

		ToXContent x = null;
		if (value instanceof SQLMethodInvokeExpr) {
			x = make(cond, name, (SQLMethodInvokeExpr) value);
		} else {
			x = make(cond, name, value);
		}

		return x;
	}

	private ToXContent make(Condition cond, String name, SQLMethodInvokeExpr value) throws SqlParseException {
		ToXContent bqb = null;
		Paramer paramer = null;
		switch (value.getMethodName()) {
		case "query":
			paramer = Paramer.parseParamer(value);
			QueryStringQueryBuilder queryString = QueryBuilders.queryString(paramer.value);
			bqb = Paramer.fullParamer(queryString, paramer);
			if (!isQuery) {
				bqb = FilterBuilders.queryFilter((QueryBuilder) bqb);
			}
			bqb = fixNot(cond, bqb);
			break;
		case "matchQuery":
			paramer = Paramer.parseParamer(value);
			MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(name, paramer.value);
			bqb = Paramer.fullParamer(matchQuery, paramer);
			if (!isQuery) {
				bqb = FilterBuilders.queryFilter((QueryBuilder) bqb);
			}
			bqb = fixNot(cond, bqb);
			break;

		case "scoreQuery":
			Float boost = Float.parseFloat(value.getParameters().get(1).toString());
			Condition subCond = new Condition(cond.getConn(), cond.getName(), cond.getOpear(), value.getParameters().get(0));
			if (isQuery) {
				bqb = QueryBuilders.constantScoreQuery((QueryBuilder) make(subCond)).boost(boost);
			} else {
				bqb = QueryBuilders.constantScoreQuery((FilterBuilder) make(subCond)).boost(boost);
				bqb = FilterBuilders.queryFilter((QueryBuilder) bqb);
			}
			break;
		case "wildcardQuery":
			paramer = Paramer.parseParamer(value);
			WildcardQueryBuilder wildcardQuery = QueryBuilders.wildcardQuery(name, paramer.value);
			bqb = Paramer.fullParamer(wildcardQuery, paramer);
			if (!isQuery) {
				bqb = FilterBuilders.queryFilter((QueryBuilder) bqb);
			}
			break;

		case "matchPhraseQuery":
			paramer = Paramer.parseParamer(value);
			MatchQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery(name, paramer.value);
			bqb = Paramer.fullParamer(matchPhraseQuery, paramer);
			if (!isQuery) {
				bqb = FilterBuilders.queryFilter((QueryBuilder) bqb);
			}
			break;
		default:
			throw new SqlParseException("it did not support this query method " + value.getMethodName());

		}

		return bqb;
	}

	private ToXContent make(Condition cond, String name, Object value) throws SqlParseException {
		ToXContent x = null;
		switch (cond.getOpear()) {
		case IS:
		case EQ:
			if (isQuery)
				x = QueryBuilders.termQuery(name, value);
			else
				x = FilterBuilders.termFilter(name, value);
			break;
		case GT:
			if (isQuery)
				x = QueryBuilders.rangeQuery(name).gt(value);
			else
				x = FilterBuilders.rangeFilter(name).gt(value);

			break;
		case GTE:
			if (isQuery)
				x = QueryBuilders.rangeQuery(name).gte(value);
			else
				x = FilterBuilders.rangeFilter(name).gte(value);
			break;
		case LT:
			if (isQuery)
				x = QueryBuilders.rangeQuery(name).lt(value);
			else
				x = FilterBuilders.rangeFilter(name).lt(value);

			break;
		case LTE:
			if (isQuery)
				x = QueryBuilders.rangeQuery(name).lte(value);
			else
				x = FilterBuilders.rangeFilter(name).lte(value);
			break;
		case NIN:
		case IN:
			if (isQuery)
				x = QueryBuilders.inQuery(name, (Object[]) value);
			else
				x = FilterBuilders.inFilter(name, (Object[]) value);

		default:
			throw new SqlParseException("not define type " + cond.getName());
		}

		x = fixNot(cond, x);
		return x;
	}

	private ToXContent fixNot(Condition cond, ToXContent bqb) {
		if (NOT_OPEAR_SET.contains(cond.getOpear())) {
			if (isQuery) {
				bqb = QueryBuilders.boolQuery().mustNot((QueryBuilder) bqb);
			} else {
				bqb = FilterBuilders.notFilter((FilterBuilder) bqb);
			}
		}
		return bqb;
	}

}
