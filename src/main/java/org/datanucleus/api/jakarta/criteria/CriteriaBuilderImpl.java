/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
   ...
**********************************************************************/
package org.datanucleus.api.jakarta.criteria;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate.BooleanOperator;

import org.datanucleus.api.jakarta.JakartaEntityManagerFactory;
import org.datanucleus.store.query.expression.CreatorExpression;
import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.SubqueryExpression;
import org.datanucleus.store.query.expression.VariableExpression;

/**
 * Implementation of Jakarta Persistence "CriteriaBuilder".
 * Acts as a factory for expressions, and also for the criteria queries themselves.
 */
public class CriteriaBuilderImpl implements CriteriaBuilder, Serializable
{
    static final long serialVersionUID = -1798682551615240941L;

    JakartaEntityManagerFactory emf;

    public CriteriaBuilderImpl(JakartaEntityManagerFactory emf)
    {
        this.emf = emf;
    }

    public JakartaEntityManagerFactory getEntityManagerFactory()
    {
        return emf;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#createQuery()
     */
    public CriteriaQuery<Object> createQuery()
    {
        return new CriteriaQueryImpl<Object>(this, null);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#createQuery(java.lang.Class)
     */
    public <T> CriteriaQuery<T> createQuery(Class<T> resultCls)
    {
        return new CriteriaQueryImpl<T>(this, resultCls);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#createTupleQuery()
     */
    public CriteriaQuery<Tuple> createTupleQuery()
    {
        return new CriteriaQueryImpl<Tuple>(this, Tuple.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#createCriteriaDelete(java.lang.Class)
     */
    public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> cls)
    {
        CriteriaDelete<T> crit = new CriteriaDeleteImpl<T>(this);
        crit.from(cls);
        return crit;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#createCriteriaUpdate(java.lang.Class)
     */
    public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> cls)
    {
        CriteriaUpdate<T> crit = new CriteriaUpdateImpl<T>(this);
        crit.from(cls);
        return crit;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#asc(jakarta.persistence.criteria.Expression)
     */
    public Order asc(Expression<?> expr)
    {
        return new OrderImpl(expr, true);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#desc(jakarta.persistence.criteria.Expression)
     */
    public Order desc(Expression<?> expr)
    {
        return new OrderImpl(expr, false);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#abs(jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> abs(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "avg", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#avg(jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<Double> avg(Expression<N> expr)
    {
        ExpressionImpl<Double> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "avg", args);
        return select;
    }

    public <N extends Number> Expression<Double> avgDistinct(Expression<N> expr)
    {
        ExpressionImpl<Double> select = new ExpressionImpl(this, expr.getJavaType());

        DyadicExpression dyExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_DISTINCT, ((ExpressionImpl)expr).getQueryExpression());
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(dyExpr);
        select.queryExpr = new InvokeExpression(null, "avg", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#count(jakarta.persistence.criteria.Expression)
     */
    public Expression<Long> count(Expression<?> expr)
    {
        // TODO Check that this expression is valid?
        ExpressionImpl<Long> select = new ExpressionImpl(this, expr.getJavaType());

        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "count", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#countDistinct(jakarta.persistence.criteria.Expression)
     */
    public Expression<Long> countDistinct(Expression<?> expr)
    {
        // TODO Check that this expression is valid?
        ExpressionImpl<Long> select = new ExpressionImpl(this, expr.getJavaType());

        DyadicExpression dyExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_DISTINCT, ((ExpressionImpl)expr).getQueryExpression());
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(dyExpr);
        select.queryExpr = new InvokeExpression(null, "count", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#max(jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> max(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "max", args);
        return select;
    }

    public <N extends Number> Expression<N> maxDistinct(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());

        DyadicExpression dyExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_DISTINCT, ((ExpressionImpl)expr).getQueryExpression());
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(dyExpr);
        select.queryExpr = new InvokeExpression(null, "max", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#greatest(jakarta.persistence.criteria.Expression)
     */
    public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> expr)
    {
        ExpressionImpl<X> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "max", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#min(jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> min(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "min", args);
        return select;
    }

    public <N extends Number> Expression<N> minDistinct(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());

        DyadicExpression dyExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_DISTINCT, ((ExpressionImpl)expr).getQueryExpression());
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(dyExpr);
        select.queryExpr = new InvokeExpression(null, "min", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#least(jakarta.persistence.criteria.Expression)
     */
    public <X extends Comparable<? super X>> Expression<X> least(Expression<X> expr)
    {
        ExpressionImpl<X> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "min", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sqrt(jakarta.persistence.criteria.Expression)
     */
    public Expression<Double> sqrt(Expression<? extends Number> expr)
    {
        ExpressionImpl<Double> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "sqrt", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sum(jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> sum(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "sum", args);
        return select;
    }

    public <N extends Number> Expression<N> sumDistinct(Expression<N> expr)
    {
        ExpressionImpl<N> select = new ExpressionImpl(this, expr.getJavaType());

        DyadicExpression dyExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_DISTINCT, ((ExpressionImpl)expr).getQueryExpression());
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList<>();
        args.add(dyExpr);
        select.queryExpr = new InvokeExpression(null, "sum", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sumAsDouble(jakarta.persistence.criteria.Expression)
     */
    public Expression<Double> sumAsDouble(Expression<Float> expr)
    {
        ExpressionImpl<Double> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "sum", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sumAsLong(jakarta.persistence.criteria.Expression)
     */
    public Expression<Long> sumAsLong(Expression<Integer> expr)
    {
        ExpressionImpl<Long> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        select.queryExpr = new InvokeExpression(null, "sum", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#and(jakarta.persistence.criteria.Predicate[])
     */
    public Predicate and(Predicate... preds)
    {
        PredicateImpl pred = new PredicateImpl(this);
        for (int i=0;i<preds.length;i++)
        {
            pred.append(preds[i]);
        }

        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#and(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate and(Expression<Boolean> expr0, Expression<Boolean> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_AND, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#or(jakarta.persistence.criteria.Predicate[])
     */
    public Predicate or(Predicate... preds)
    {
        PredicateImpl pred = new PredicateImpl(this, BooleanOperator.OR);
        for (int i=0;i<preds.length;i++)
        {
            pred.append(preds[i]);
        }

        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#or(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate or(Expression<Boolean> expr0, Expression<Boolean> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this, BooleanOperator.OR);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_OR, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#equal(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate equal(Expression<?> expr0, Expression<?> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_EQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#equal(jakarta.persistence.criteria.Expression, java.lang.Object)
     */
    public Predicate equal(Expression<?> expr, Object obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_EQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notEqual(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate notEqual(Expression<?> expr0, Expression<?> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_NOTEQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notEqual(jakarta.persistence.criteria.Expression, java.lang.Object)
     */
    public Predicate notEqual(Expression<?> expr, Object obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_NOTEQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isNotNull(jakarta.persistence.criteria.Expression)
     */
    public Predicate isNotNull(Expression<?> expr)
    {
        return expr.isNotNull();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isNull(jakarta.persistence.criteria.Expression)
     */
    public Predicate isNull(Expression<?> expr)
    {
        return expr.isNull();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#ge(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate ge(Expression<? extends Number> expr0, Expression<? extends Number> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GTEQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#ge(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public Predicate ge(Expression<? extends Number> expr, Number obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GTEQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#greaterThan(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> expr0, Expression<? extends Y> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GT, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#greaterThan(jakarta.persistence.criteria.Expression, java.lang.Comparable)
     */
    public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> expr, Y obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GT, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#greaterThanOrEqualTo(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> expr0, Expression<? extends Y> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GTEQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#greaterThanOrEqualTo(jakarta.persistence.criteria.Expression, java.lang.Comparable)
     */
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> expr, Y obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GTEQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#gt(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate gt(Expression<? extends Number> expr0, Expression<? extends Number> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GT, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#gt(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public Predicate gt(Expression<? extends Number> expr, Number obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_GT, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#le(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate le(Expression<? extends Number> expr0, Expression<? extends Number> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LTEQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#le(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public Predicate le(Expression<? extends Number> expr, Number obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LTEQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lessThan(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> expr0, Expression<? extends Y> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LT, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lessThan(jakarta.persistence.criteria.Expression, java.lang.Comparable)
     */
    public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> expr, Y obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LT, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lessThanOrEqualTo(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> expr0, Expression<? extends Y> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LTEQ, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lessThanOrEqualTo(jakarta.persistence.criteria.Expression, java.lang.Comparable)
     */
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> expr, Y obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LTEQ, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lt(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate lt(Expression<? extends Number> expr0, Expression<? extends Number> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LT, ((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lt(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public Predicate lt(Expression<? extends Number> expr, Number obj)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_LT, new Literal(obj));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sum(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> sum(Expression<? extends N> expr0, Expression<? extends N> expr1)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr0.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_ADD, ((ExpressionImpl)expr1).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sum(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public <N extends Number> Expression<N> sum(Expression<? extends N> expr, N obj)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_ADD, new Literal(obj));
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#sum(java.lang.Number, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> sum(N obj, Expression<? extends N> expr)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(new Literal(obj), org.datanucleus.store.query.expression.Expression.OP_ADD, ((ExpressionImpl)expr).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#quot(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<Number> quot(Expression<? extends Number> expr0, Expression<? extends Number> expr1)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Number>(this, Number.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_DIV, ((ExpressionImpl)expr1).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#quot(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public Expression<Number> quot(Expression<? extends Number> expr, Number obj)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Number>(this, Number.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_DIV, new Literal(obj));
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#quot(java.lang.Number, jakarta.persistence.criteria.Expression)
     */
    public Expression<Number> quot(Number obj, Expression<? extends Number> expr)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Number>(this, Number.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(new Literal(obj), org.datanucleus.store.query.expression.Expression.OP_DIV, ((ExpressionImpl)expr).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#diff(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> diff(Expression<? extends N> expr0, Expression<? extends N> expr1)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>)expr0.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_SUB, ((ExpressionImpl)expr1).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#diff(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public <N extends Number> Expression<N> diff(Expression<? extends N> expr, N obj)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_SUB, new Literal(obj));
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#diff(java.lang.Number, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> diff(N obj, Expression<? extends N> expr)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(new Literal(obj), org.datanucleus.store.query.expression.Expression.OP_SUB, ((ExpressionImpl)expr).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#prod(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> prod(Expression<? extends N> expr0, Expression<? extends N> expr1)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>)expr0.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_MUL, ((ExpressionImpl)expr1).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#prod(jakarta.persistence.criteria.Expression, java.lang.Number)
     */
    public <N extends Number> Expression<N> prod(Expression<? extends N> expr, N obj)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_MUL, new Literal(obj));
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#prod(java.lang.Number, jakarta.persistence.criteria.Expression)
     */
    public <N extends Number> Expression<N> prod(N obj, Expression<? extends N> expr)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(new Literal(obj), org.datanucleus.store.query.expression.Expression.OP_MUL, ((ExpressionImpl)expr).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#mod(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> mod(Expression<Integer> expr0, Expression<Integer> expr1)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Integer>(this, Integer.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr0).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_MOD, ((ExpressionImpl)expr1).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#mod(jakarta.persistence.criteria.Expression, java.lang.Integer)
     */
    public Expression<Integer> mod(Expression<Integer> expr, Integer obj)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Integer>(this, Integer.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_MOD, new Literal(obj));
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#mod(java.lang.Integer, jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> mod(Integer obj, Expression<Integer> expr)
    {
        ExpressionImpl sumExpr = new ExpressionImpl<Integer>(this, Integer.class);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(new Literal(obj), org.datanucleus.store.query.expression.Expression.OP_MOD, ((ExpressionImpl)expr).getQueryExpression());
        sumExpr.queryExpr = queryExpr;
        return sumExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#between(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> expr0, Expression<? extends Y> expr1,
            Expression<? extends Y> expr2)
    {
        PredicateImpl pred = new PredicateImpl(this);

        org.datanucleus.store.query.expression.Expression theExpr = ((ExpressionImpl)expr0).getQueryExpression();
        org.datanucleus.store.query.expression.Expression lowerExpr = ((ExpressionImpl)expr1).getQueryExpression();
        org.datanucleus.store.query.expression.Expression upperExpr = ((ExpressionImpl)expr2).getQueryExpression();
        DyadicExpression lowerDyadic =
            new DyadicExpression(theExpr, org.datanucleus.store.query.expression.Expression.OP_GTEQ, lowerExpr);
        DyadicExpression upperDyadic =
            new DyadicExpression(theExpr, org.datanucleus.store.query.expression.Expression.OP_LTEQ, upperExpr);
        DyadicExpression overallDyadic =
            new DyadicExpression(lowerDyadic, org.datanucleus.store.query.expression.Expression.OP_AND, upperDyadic);
        pred.queryExpr = overallDyadic;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#between(jakarta.persistence.criteria.Expression, java.lang.Comparable, java.lang.Comparable)
     */
    public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> expr, Y obj0, Y obj1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression theExpr = ((ExpressionImpl)expr).getQueryExpression();
        DyadicExpression lowerDyadic = new DyadicExpression(theExpr, org.datanucleus.store.query.expression.Expression.OP_GTEQ, new Literal(obj0));
        DyadicExpression upperDyadic = new DyadicExpression(theExpr, org.datanucleus.store.query.expression.Expression.OP_LTEQ, new Literal(obj1));
        DyadicExpression overallDyadic = new DyadicExpression(lowerDyadic, org.datanucleus.store.query.expression.Expression.OP_AND, upperDyadic);
        pred.queryExpr = overallDyadic;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#coalesce()
     */
    public <T> Coalesce<T> coalesce()
    {
        return new CoalesceExpression(this, Object.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#coalesce(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y> Expression<Y> coalesce(Expression<? extends Y> expr0, Expression<? extends Y> expr1)
    {
        CoalesceExpression<Y> coalesceExpr = new CoalesceExpression<Y>(this, (Class<Y>) expr0.getJavaType());
        coalesceExpr.value(expr0);
        coalesceExpr.value(expr1);
        return coalesceExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#coalesce(jakarta.persistence.criteria.Expression, java.lang.Object)
     */
    public <Y> Expression<Y> coalesce(Expression<? extends Y> expr, Y val)
    {
        CoalesceExpression<Y> coalesceExpr = new CoalesceExpression<Y>(this, (Class<Y>) expr.getJavaType());
        coalesceExpr.value(expr);
        coalesceExpr.value(val);
        return coalesceExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#nullif(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <Y> Expression<Y> nullif(Expression<Y> expr0, Expression<?> expr1)
    {
        ExpressionImpl<Y> coalExpr = new ExpressionImpl<Y>(this, (Class<Y>) expr0.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr0).getQueryExpression());
        args.add(((ExpressionImpl)expr1).getQueryExpression());
        coalExpr.queryExpr = new InvokeExpression(null, "NULLIF", args);
        return coalExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#nullif(jakarta.persistence.criteria.Expression, java.lang.Object)
     */
    public <Y> Expression<Y> nullif(Expression<Y> expr, Y val)
    {
        ExpressionImpl<Y> coalExpr = new ExpressionImpl<Y>(this, (Class<Y>)expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        args.add(new Literal(val));
        coalExpr.queryExpr = new InvokeExpression(null, "NULLIF", args);
        return coalExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#conjunction()
     */
    public Predicate conjunction()
    {
        PredicateImpl pred = new PredicateImpl(this);
        pred.queryExpr = new Literal(Boolean.TRUE);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#disjunction()
     */
    public Predicate disjunction()
    {
        PredicateImpl pred = new PredicateImpl(this);
        pred.queryExpr = new Literal(Boolean.FALSE);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#construct(java.lang.Class, jakarta.persistence.criteria.Selection<?>[])
     */
    public <Y> CompoundSelection<Y> construct(Class<Y> cls, Selection<?>... args)
    {
        CompoundSelectionImpl<Y> select = new CompoundSelectionImpl<Y>(this, cls, args);
        List<String> clsNameComponents = new ArrayList();
        StringTokenizer tok = new StringTokenizer(cls.getName(), ".");
        while (tok.hasMoreTokens())
        {
            clsNameComponents.add(tok.nextToken());
        }
        List<org.datanucleus.store.query.expression.Expression> ctrArgs = new ArrayList();
        if (args != null)
        {
            for (int i=0;i<args.length;i++)
            {
                ExpressionImpl argExpr = (ExpressionImpl) args[i];
                ctrArgs.add(argExpr.queryExpr);
            }
        }
        select.queryExpr = new CreatorExpression(clsNameComponents, ctrArgs);
        return select;
    }

    @Override
    public Expression<Date> currentDate()
    {
        ExpressionImpl<Date> select = new ExpressionImpl(this, Date.class);
        select.queryExpr = new InvokeExpression(null, "CURRENT_DATE", null);
        return select;
    }

    @Override
    public Expression<Time> currentTime()
    {
        ExpressionImpl<Time> select = new ExpressionImpl(this, Time.class);
        select.queryExpr = new InvokeExpression(null, "CURRENT_TIME", null);
        return select;
    }

    @Override
    public Expression<Timestamp> currentTimestamp()
    {
        ExpressionImpl<Timestamp> select = new ExpressionImpl(this, Timestamp.class);
        select.queryExpr = new InvokeExpression(null, "CURRENT_TIMESTAMP", null);
        return select;
    }

    @Override
    public Expression<java.time.LocalDate> localDate()
    {
        ExpressionImpl<LocalDate> select = new ExpressionImpl(this, LocalDate.class);
        select.queryExpr = new InvokeExpression(null, "LOCAL_DATE", null);
        return select;
    }

    @Override
    public Expression<java.time.LocalDateTime> localDateTime()
    {
        ExpressionImpl<LocalDateTime> select = new ExpressionImpl(this, LocalDateTime.class);
        select.queryExpr = new InvokeExpression(null, "LOCAL_DATETIME", null);
        return select;
    }

    @Override
    public Expression<java.time.LocalTime> localTime()
    {
        ExpressionImpl<LocalTime> select = new ExpressionImpl(this, LocalTime.class);
        select.queryExpr = new InvokeExpression(null, "LOCAL_TIME", null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#function(java.lang.String, java.lang.Class, jakarta.persistence.criteria.Expression<?>[])
     */
    public <T> Expression<T> function(String funcName, Class<T> returnType, Expression<?>... argExprs)
    {
        // Call method "SQL_function" with the funcName as first argument
        ExpressionImpl<T> funcExpr = new ExpressionImpl(this, returnType);
        List<org.datanucleus.store.query.expression.Expression> args = new ArrayList();
        args.add(new Literal(funcName));
        if (argExprs != null)
        {
            for (int i=0;i<argExprs.length;i++)
            {
                args.add(((ExpressionImpl<?>)argExprs[i]).getQueryExpression());
            }
        }
        funcExpr.queryExpr = new InvokeExpression(null, "SQL_function", args);
        return funcExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#all(jakarta.persistence.criteria.Subquery)
     */
    public <Y> Expression<Y> all(Subquery<Y> sub)
    {
        ExpressionImpl<Y> allExpr = new ExpressionImpl<Y>(this, (Class<Y>) sub.getJavaType());
        org.datanucleus.store.query.expression.Expression subExpr = ((SubqueryImpl<Y>)sub).getQueryExpression();
        String varName = null;
        if (subExpr instanceof VariableExpression)
        {
            varName = ((VariableExpression)subExpr).getId();
        }
        else
        {
            varName = "SUB" + SubqueryImpl.random.nextInt();
        }
        allExpr.queryExpr = new SubqueryExpression("ALL", new VariableExpression(varName));
        return allExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#any(jakarta.persistence.criteria.Subquery)
     */
    public <Y> Expression<Y> any(Subquery<Y> sub)
    {
        ExpressionImpl<Y> allExpr = new ExpressionImpl<Y>(this, (Class<Y>) sub.getJavaType());
        org.datanucleus.store.query.expression.Expression subExpr = ((SubqueryImpl<Y>)sub).getQueryExpression();
        String varName = null;
        if (subExpr instanceof VariableExpression)
        {
            varName = ((VariableExpression)subExpr).getId();
        }
        else
        {
            varName = "SUB" + SubqueryImpl.random.nextInt();
        }
        allExpr.queryExpr = new SubqueryExpression("ANY", new VariableExpression(varName));
        return allExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#some(jakarta.persistence.criteria.Subquery)
     */
    public <Y> Expression<Y> some(Subquery<Y> sub)
    {
        ExpressionImpl<Y> allExpr = new ExpressionImpl<Y>(this, (Class<Y>) sub.getJavaType());
        org.datanucleus.store.query.expression.Expression subExpr = ((SubqueryImpl<Y>)sub).getQueryExpression();
        String varName = null;
        if (subExpr instanceof VariableExpression)
        {
            varName = ((VariableExpression)subExpr).getId();
        }
        else
        {
            varName = "SUB" + SubqueryImpl.random.nextInt();
        }
        allExpr.queryExpr = new SubqueryExpression("SOME", new VariableExpression(varName));
        return allExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#exists(jakarta.persistence.criteria.Subquery)
     */
    public Predicate exists(Subquery<?> sub)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression subExpr = ((SubqueryImpl<?>)sub).getQueryExpression();
        String varName = null;
        if (subExpr instanceof VariableExpression)
        {
            varName = ((VariableExpression)subExpr).getId();
        }
        else
        {
            varName = "SUB" + SubqueryImpl.random.nextInt();
        }
        pred.queryExpr = new SubqueryExpression("EXISTS", new VariableExpression(varName));
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#in(jakarta.persistence.criteria.Expression)
     */
    public <X> In<X> in(Expression<? extends X> expr)
    {
        return new InPredicate<X>(this, expr);
    }

    public <X> In<X> in(Expression<? extends X> expr, Expression<? extends X>... values)
    {
        return new InPredicate<X>(this, expr, values);
    }

    public <X> In<X> in(Expression<? extends X> expr, X... values) 
    {
        return new InPredicate<X>(this, expr, values);
    }

    public <X> In<X> in(Expression<? extends X> expr, Collection<X> values) 
    {
        return new InPredicate<X>(this, expr, values);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isEmpty(jakarta.persistence.criteria.Expression)
     */
    public <C extends Collection<?>> Predicate isEmpty(Expression<C> collExpr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr = new InvokeExpression(((ExpressionImpl)collExpr).getQueryExpression(), "isEmpty", null);
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isMember(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <E, C extends Collection<E>> Predicate isMember(Expression<E> expr, Expression<C> collExpr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr).getQueryExpression());
        org.datanucleus.store.query.expression.Expression queryExpr = new InvokeExpression(((ExpressionImpl)collExpr).getQueryExpression(), "contains", args);
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isMember(java.lang.Object, jakarta.persistence.criteria.Expression)
     */
    public <E, C extends Collection<E>> Predicate isMember(E val, Expression<C> collExpr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(new Literal(val));
        org.datanucleus.store.query.expression.Expression queryExpr = new InvokeExpression(((ExpressionImpl)collExpr).getQueryExpression(), "contains", args);
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isNotEmpty(jakarta.persistence.criteria.Expression)
     */
    public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> collExpr)
    {
        return isEmpty(collExpr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isNotMember(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> expr, Expression<C> collExpr)
    {
        return isMember(expr, collExpr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isNotMember(java.lang.Object, jakarta.persistence.criteria.Expression)
     */
    public <E, C extends Collection<E>> Predicate isNotMember(E val, Expression<C> collExpr)
    {
        return isMember(val, collExpr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#size(jakarta.persistence.criteria.Expression)
     */
    public <C extends Collection<?>> Expression<Integer> size(Expression<C> expr)
    {
        ExpressionImpl<Integer> collSizeExpr = new ExpressionImpl(this, expr.getJavaType());
        collSizeExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "size", null);
        return collSizeExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#size(java.util.Collection)
     */
    public <C extends Collection<?>> Expression<Integer> size(C coll)
    {
        // Strange method that seemingly just returns the size of the input collection, so why have it?
        ExpressionImpl<Integer> collSizeExpr = new ExpressionImpl<Integer>(this, Integer.class);
        collSizeExpr.queryExpr = new Literal(coll.size());
        return collSizeExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isFalse(jakarta.persistence.criteria.Expression)
     */
    public Predicate isFalse(Expression<Boolean> expr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_EQ, new Literal(Boolean.FALSE));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#isTrue(jakarta.persistence.criteria.Expression)
     */
    public Predicate isTrue(Expression<Boolean> expr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        org.datanucleus.store.query.expression.Expression queryExpr =
            new DyadicExpression(((ExpressionImpl)expr).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_EQ, new Literal(Boolean.TRUE));
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#tuple(jakarta.persistence.criteria.Selection<?>[])
     */
    public CompoundSelection<Tuple> tuple(Selection<?>... selections)
    {
        return new CompoundSelectionImpl<Tuple>(this, Tuple.class, selections);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#array(jakarta.persistence.criteria.Selection<?>[])
     */
    public CompoundSelection<Object[]> array(Selection<?>... selections)
    {
        return new CompoundSelectionImpl(this, Object[].class, selections);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#keys(java.util.Map)
     */
    public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "CriteriaBuilder.keys not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#values(java.util.Map)
     */
    public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "CriteriaBuilder.values not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate like(Expression<String> expr, Expression<String> expr1)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr1).getQueryExpression());
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, java.lang.String)
     */
    public Predicate like(Expression<String> expr, String regex)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(new Literal(regex));
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate like(Expression<String> expr, Expression<String> expr1, Expression<Character> escExpr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr1).getQueryExpression());
        args.add(((ExpressionImpl)escExpr).getQueryExpression());
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, char)
     */
    public Predicate like(Expression<String> expr, Expression<String> expr1, char escChr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(((ExpressionImpl)expr1).getQueryExpression());
        args.add(new Literal(escChr));
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, java.lang.String, jakarta.persistence.criteria.Expression)
     */
    public Predicate like(Expression<String> expr, String regex, Expression<Character> escExpr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(new Literal(regex));
        args.add(((ExpressionImpl)escExpr).getQueryExpression());
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#like(jakarta.persistence.criteria.Expression, java.lang.String, char)
     */
    public Predicate like(Expression<String> expr, String regex, char escChr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        List args = new ArrayList();
        args.add(new Literal(regex));
        args.add(new Literal(escChr));
        pred.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "matches", args);
        return pred;
    }

    @Override
    public <N extends Number> Expression<N> neg(Expression<N> expr)
    {
        ExpressionImpl<N> negExpr = new ExpressionImpl<N>(this, (Class<N>) expr.getJavaType());
        negExpr.queryExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_NEG, ((ExpressionImpl)expr).getQueryExpression());
        return negExpr;
    }

    @Override
    public Expression<Integer> sign(Expression<? extends Number> expr)
    {
        ExpressionImpl<Integer> signExpr = new ExpressionImpl(this, Integer.class);
        signExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "sign", null);
        return signExpr;
    }

    @Override
    public Predicate not(Expression<Boolean> expr)
    {
        PredicateImpl pred = new PredicateImpl(this);
        pred.queryExpr = new DyadicExpression(org.datanucleus.store.query.expression.Expression.OP_NOT, ((ExpressionImpl)expr).getQueryExpression());
        return pred;
    }

    @Override
    public Predicate notLike(Expression<String> expr, Expression<String> expr1)
    {
        return like(expr, expr1).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notLike(jakarta.persistence.criteria.Expression, java.lang.String)
     */
    public Predicate notLike(Expression<String> expr, String regex)
    {
        return like(expr, regex).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notLike(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Predicate notLike(Expression<String> expr, Expression<String> expr1, Expression<Character> escExpr)
    {
        return like(expr, expr1, escExpr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notLike(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, char)
     */
    public Predicate notLike(Expression<String> expr, Expression<String> expr1, char escChr)
    {
        return like(expr, expr1, escChr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notLike(jakarta.persistence.criteria.Expression, java.lang.String, jakarta.persistence.criteria.Expression)
     */
    public Predicate notLike(Expression<String> expr, String regex, Expression<Character> escExpr)
    {
        return like(expr, regex, escExpr).not();
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#notLike(jakarta.persistence.criteria.Expression, java.lang.String, char)
     */
    public Predicate notLike(Expression<String> expr, String regex, char escChr)
    {
        return like(expr, regex, escChr).not();
    }

    private static long PARAM_NUMBER = 0;

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#parameter(java.lang.Class)
     */
    public <T> ParameterExpression<T> parameter(Class<T> cls)
    {
        // No name specified so add a dummy name
        String paramName = "DN_PARAM_" + (PARAM_NUMBER++);
        ParameterExpressionImpl<T> param = new ParameterExpressionImpl<T>(this, cls, paramName);
        param.queryExpr = new org.datanucleus.store.query.expression.ParameterExpression(paramName, cls);
        return param;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#parameter(java.lang.Class, java.lang.String)
     */
    public <T> ParameterExpression<T> parameter(Class<T> cls, String name)
    {
        ParameterExpressionImpl<T> param = new ParameterExpressionImpl<T>(this, cls, name);
        param.queryExpr = new org.datanucleus.store.query.expression.ParameterExpression(name, cls);
        return param;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#selectCase()
     */
    public <R> Case<R> selectCase()
    {
        CaseExpressionImpl<R> caseExpr = new CaseExpressionImpl<>(this);
        return caseExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#selectCase(jakarta.persistence.criteria.Expression)
     */
    public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expr)
    {
        SimpleCaseExpressionImpl<C, R> caseExpr = new SimpleCaseExpressionImpl(this, (ExpressionImpl) expr);
        return caseExpr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toBigDecimal(jakarta.persistence.criteria.Expression)
     */
    public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> expr)
    {
        return expr.as(BigDecimal.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toBigInteger(jakarta.persistence.criteria.Expression)
     */
    public Expression<BigInteger> toBigInteger(Expression<? extends Number> expr)
    {
        return expr.as(BigInteger.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toDouble(jakarta.persistence.criteria.Expression)
     */
    public Expression<Double> toDouble(Expression<? extends Number> expr)
    {
        return expr.as(Double.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toFloat(jakarta.persistence.criteria.Expression)
     */
    public Expression<Float> toFloat(Expression<? extends Number> expr)
    {
        return expr.as(Float.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toInteger(jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> toInteger(Expression<? extends Number> expr)
    {
        return expr.as(Integer.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toLong(jakarta.persistence.criteria.Expression)
     */
    public Expression<Long> toLong(Expression<? extends Number> expr)
    {
        return expr.as(Long.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#toString(jakarta.persistence.criteria.Expression)
     */
    public Expression<String> toString(Expression<Character> expr)
    {
        return expr.as(String.class);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#concat(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> concat(Expression<String> expr0, Expression<String> expr1)
    {
        return new ConcatExpression(this, expr0, expr1);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#concat(jakarta.persistence.criteria.Expression, java.lang.String)
     */
    public Expression<String> concat(Expression<String> expr, String val)
    {
        return new ConcatExpression(this, expr, new LiteralExpression<String>(this, val));
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#concat(java.lang.String, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> concat(String val, Expression<String> expr)
    {
        return new ConcatExpression(this, new LiteralExpression<String>(this, val), expr);
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#locate(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> locate(Expression<String> expr, Expression<String> exprSubstr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, Integer.class);
        List args = new ArrayList();
        args.add(((ExpressionImpl)exprSubstr).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "indexOf", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#locate(jakarta.persistence.criteria.Expression, java.lang.String)
     */
    public Expression<Integer> locate(Expression<String> expr, String substr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, Integer.class);
        List args = new ArrayList();
        args.add(new Literal(substr));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "indexOf", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#locate(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> locate(Expression<String> expr, Expression<String> exprSubstr, Expression<Integer> exprPos)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, Integer.class);
        List args = new ArrayList();
        args.add(((ExpressionImpl)exprSubstr).getQueryExpression());
        args.add(((ExpressionImpl)exprPos).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "indexOf", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#locate(jakarta.persistence.criteria.Expression, java.lang.String, int)
     */
    public Expression<Integer> locate(Expression<String> expr, String substr, int pos)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, Integer.class);
        List args = new ArrayList();
        args.add(new Literal(substr));
        args.add(new Literal(pos));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "indexOf", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#substring(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> substring(Expression<String> expr, Expression<Integer> posExpr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)posExpr).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "substring", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#substring(jakarta.persistence.criteria.Expression, int)
     */
    public Expression<String> substring(Expression<String> expr, int pos)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(new Literal(pos));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "substring", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#substring(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> substring(Expression<String> expr, Expression<Integer> posExpr0, Expression<Integer> posExpr1)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)posExpr0).getQueryExpression());
        args.add(((ExpressionImpl)posExpr1).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "substring", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#substring(jakarta.persistence.criteria.Expression, int, int)
     */
    public Expression<String> substring(Expression<String> expr, int pos0, int pos1)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(new Literal(pos0));
        args.add(new Literal(pos1));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "substring", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "trim", null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(jakarta.persistence.criteria.CriteriaBuilder.Trimspec, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(Trimspec spec, Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        String method = "trim";
        if (spec == Trimspec.LEADING)
        {
            method = "trimLeft";
        }
        else if (spec == Trimspec.TRAILING)
        {
            method = "trimRight";
        }
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), method, null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(Expression<Character> chr, Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(((ExpressionImpl)chr).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "trim", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(char, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(char chr, Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        List args = new ArrayList();
        args.add(new Literal(chr));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "trim", args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(jakarta.persistence.criteria.CriteriaBuilder.Trimspec, jakarta.persistence.criteria.Expression, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(Trimspec spec, Expression<Character> chr, Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        String method = "trim";
        if (spec == Trimspec.LEADING)
        {
            method = "trimLeft";
        }
        else if (spec == Trimspec.TRAILING)
        {
            method = "trimRight";
        }
        List args = new ArrayList();
        args.add(((ExpressionImpl)chr).getQueryExpression());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), method, args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#trim(jakarta.persistence.criteria.CriteriaBuilder.Trimspec, char, jakarta.persistence.criteria.Expression)
     */
    public Expression<String> trim(Trimspec spec, char chr, Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        String method = "trim";
        if (spec == Trimspec.LEADING)
        {
            method = "trimLeft";
        }
        else if (spec == Trimspec.TRAILING)
        {
            method = "trimRight";
        }
        List args = new ArrayList();
        args.add(new Literal(chr));
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), method, args);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#lower(jakarta.persistence.criteria.Expression)
     */
    public Expression<String> lower(Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "toLowerCase", null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#upper(jakarta.persistence.criteria.Expression)
     */
    public Expression<String> upper(Expression<String> expr)
    {
        ExpressionImpl<String> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "toUpperCase", null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#length(jakarta.persistence.criteria.Expression)
     */
    public Expression<Integer> length(Expression<String> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, Integer.class);
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "length", null);
        return select;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#literal(java.lang.Object)
     */
    public <T> Expression<T> literal(T obj)
    {
        ExpressionImpl expr = new ExpressionImpl<T>(this, (Class<T>) obj.getClass());
        expr.queryExpr = new Literal(obj);
        return expr;
    }

    /* (non-Javadoc)
     * @see jakarta.persistence.criteria.CriteriaBuilder#nullLiteral(java.lang.Class)
     */
    public <T> Expression<T> nullLiteral(Class<T> cls)
    {
        ExpressionImpl expr = new ExpressionImpl<T>(this, cls);
        expr.queryExpr = new Literal(null);
        return expr;
    }

    public <X, T, V extends T> Join<X, V> treat(Join<X, T> join, Class<V> type)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
                "CriteriaBuilder.treat(Join, Class) not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public <X, T, E extends T> CollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
                "CriteriaBuilder.treat(CollectionJoin, Class) not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public <X, T, E extends T> SetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
                "CriteriaBuilder.treat(SetJoin, Class) not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public <X, T, E extends T> ListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
                "CriteriaBuilder.treat(ListJoin, Class) not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public <X, K, T, V extends T> MapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
                "CriteriaBuilder.treat(MapJoin, Class) not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public <X, T extends X> Path<T> treat(Path<X> path, Class<T> type)
    {
        return new TreatPathImpl(this, (PathImpl) path, type);
    }

    public <X, T extends X> Root<T> treat(Root<X> root, Class<T> type)
    {
        return new TreatRootImpl(this, (RootImpl) root, type);
    }

    // ---------------------- DN EXTENSIONS -----------------------

    public Expression<Integer> year(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getYear", null);
        return select;
    }

    public Expression<Integer> month(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getMonth", null);
        return select;
    }

    public Expression<Integer> day(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getDay", null);
        return select;
    }

    public Expression<Integer> hour(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getHour", null);
        return select;
    }

    public Expression<Integer> minute(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getMinute", null);
        return select;
    }

    public Expression<Integer> second(Expression<? extends java.util.Date> expr)
    {
        ExpressionImpl<Integer> select = new ExpressionImpl(this, expr.getJavaType());
        select.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "getSecond", null);
        return select;
    }

    @Override
    public Expression<Double> power(Expression<? extends Number> expr, Expression<? extends Number> pow)
    {
        ExpressionImpl<Double> powerExpr = new ExpressionImpl(this, Double.class);
        List<org.datanucleus.store.query.expression.Expression> args = null;
        if (pow != null)
        {
            args = new ArrayList(1);
            args.add(new Literal(pow));
        }
        powerExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "power", args);
        return powerExpr;
    }

    @Override
    public Expression<Double> power(Expression<? extends Number> expr, Number pow)
    {
        ExpressionImpl<Double> powerExpr = new ExpressionImpl(this, Double.class);
        List<org.datanucleus.store.query.expression.Expression> args = null;
        if (pow != null)
        {
            args = new ArrayList(1);
            args.add(new Literal(pow));
        }
        powerExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "power", args);
        return powerExpr;
    }

    @Override
    public <T extends Number> Expression<T> round(Expression<T> expr, Integer digits)
    {
        ExpressionImpl<T> roundedExpr = new ExpressionImpl(this, expr.getJavaType());
        List<org.datanucleus.store.query.expression.Expression> args = null;
        if (digits != null)
        {
            args = new ArrayList(1);
            args.add(new Literal(digits));
        }
        roundedExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "round", args);
        return roundedExpr;
    }

    public Expression<Number> cos(Expression<Number> expr)
    {
        ExpressionImpl<Number> cosExpr = new ExpressionImpl(this, Number.class);
        cosExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "cos", null);
        return cosExpr;
    }

    public Expression<Number> sin(Expression<Number> expr)
    {
        ExpressionImpl<Number> sinExpr = new ExpressionImpl(this, Number.class);
        sinExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "sin", null);
        return sinExpr;
    }

    public Expression<Number> tan(Expression<Number> expr)
    {
        ExpressionImpl<Number> tanExpr = new ExpressionImpl(this, Number.class);
        tanExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "tan", null);
        return tanExpr;
    }

    public Expression<Number> acos(Expression<Number> expr)
    {
        ExpressionImpl<Number> acosExpr = new ExpressionImpl(this, Number.class);
        acosExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "acos", null);
        return acosExpr;
    }

    public Expression<Number> asin(Expression<Number> expr)
    {
        ExpressionImpl<Number> asinExpr = new ExpressionImpl(this, Number.class);
        asinExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "asin", null);
        return asinExpr;
    }

    public Expression<Number> atan(Expression<Number> expr)
    {
        ExpressionImpl<Number> atanExpr = new ExpressionImpl(this, Number.class);
        atanExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "atan", null);
        return atanExpr;
    }

    @Override
    public Expression<Double> ln(Expression<? extends Number> expr)
    {
        ExpressionImpl<Double> lnExpr = new ExpressionImpl(this, Double.class);
        lnExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "ln", null);
        return lnExpr;
    }

    // DN extension
    public Expression<Number> log(Expression<Number> expr)
    {
        ExpressionImpl<Number> logExpr = new ExpressionImpl(this, Number.class);
        logExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "log", null);
        return logExpr;
    }

    @Override
    public Expression<Double> exp(Expression<? extends Number> expr)
    {
        ExpressionImpl<Double> expExpr = new ExpressionImpl(this, Double.class);
        expExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "exp", null);
        return expExpr;
    }

    @Override
    public <N extends Number> Expression<N> ceiling(Expression<N> expr)
    {
        ExpressionImpl<N> ceilExpr = new ExpressionImpl(this, Integer.class);
        ceilExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "ceil", null);
        return ceilExpr;
    }

    @Override
    public <N extends Number> Expression<N> floor(Expression<N> expr)
    {
        ExpressionImpl<N> floorExpr = new ExpressionImpl(this, Integer.class);
        floorExpr.queryExpr = new InvokeExpression(((ExpressionImpl)expr).getQueryExpression(), "floor", null);
        return floorExpr;
    }
}