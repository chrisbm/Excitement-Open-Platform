package ac.biu.nlp.nlp.engineml.operations.rules;

import eu.excitementproject.eop.common.codeannotations.NotThreadSafe;
import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;
import ac.biu.nlp.nlp.engineml.datastructures.LemmaAndPos;
import ac.biu.nlp.nlp.engineml.rteflow.systems.Constants;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.general.Cache;
import ac.biu.nlp.nlp.general.CacheFactory;
import ac.biu.nlp.nlp.representation.PartOfSpeech;

/**
 * A {@link LexicalRuleBase} which returns rule that match a given lemma-and-part-of-speech
 * as left-hand-side. This rule-base stores the rules in a cache to save time.
 * <P><B>
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<BR>
 * !!!!    NOT THREAD SAFE   !!!!!!<BR>
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<BR>
 * </B>
 * 
 * 
 * 
 * @author Asher Stern
 * @since Jul 5, 2011
 *
 */
@NotThreadSafe
public abstract class ByLemmaPosLexicalRuleBaseWithCache<T extends LexicalRule> extends ByLemmaPosLexicalRuleBase<T>
{
	public void setCacheCapacity(int cacheCapacity)
	{
		cache = new CacheFactory<LemmaAndPos, ImmutableSet<T>>().getCache(cacheCapacity);
	}
	
	@Override
	public ImmutableSet<T> getRules(String lhsLemma, PartOfSpeech lhsPos) throws RuleBaseException
	{
		try
		{
			ImmutableSet<T> ret = null;
			LemmaAndPos lemmaAndPos = new LemmaAndPos(lhsLemma, lhsPos);
			if (cache.containsKey(lemmaAndPos))
			{
				ret = cache.get(lemmaAndPos);
			}
			else
			{
				ret = getRulesNotInCache(lhsLemma,lhsPos);
				cache.put(lemmaAndPos, ret);
			}
			return ret;
		}
		catch(TeEngineMlException e)
		{
			throw new RuleBaseException("An error occured for the given lemma and part-of-speech. See nested exception",e);
		}
	}
	
	protected abstract ImmutableSet<T> getRulesNotInCache(String lhsLemma, PartOfSpeech lhsPos) throws RuleBaseException;
	
	protected Cache<LemmaAndPos, ImmutableSet<T>> cache =
		new CacheFactory<LemmaAndPos, ImmutableSet<T>>().getCache(Constants.DEFAULT_LEXICAL_RESOURCES_CACHE_SIZE);
}
