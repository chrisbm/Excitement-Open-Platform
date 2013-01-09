package ac.biu.nlp.nlp.engineml.operations.rules.lexicalchain.builder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.excitementproject.eop.common.datastructures.SimpleValueSetMap;
import eu.excitementproject.eop.common.datastructures.ValueSetMap;
import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;

import ac.biu.nlp.nlp.engineml.datastructures.CanonicalLemmaAndPos;
import ac.biu.nlp.nlp.engineml.datastructures.LemmaAndPos;
import ac.biu.nlp.nlp.engineml.operations.rules.lexicalchain.ChainOfLexicalRules;
import ac.biu.nlp.nlp.engineml.rteflow.macro.DefaultOperationScript;
import ac.biu.nlp.nlp.engineml.script.OperationsScript;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.engineml.utilities.safemodel.SafeSamplesUtils;
import ac.biu.nlp.nlp.general.Cache;
import ac.biu.nlp.nlp.general.CacheFactory;
import ac.biu.nlp.nlp.lexical_resource.LexicalResource;
import ac.biu.nlp.nlp.lexical_resource.LexicalResourceException;
import ac.biu.nlp.nlp.lexical_resource.RuleInfo;

/**
 * Builds lexical chains whose right-hand-sides are the given words.
 * 
 * <B>NOT THREAD SAFE</B>
 * 
 * The given words should be the hypothesis words. The class uses
 * caching of the hypothesis words, and chains built for them, to save time.
 * 
 * @author Asher Stern
 * @since Jan 20, 2012
 *
 */
public class BuilderSetOfWords
{
	public static final int CACHE_CAPACITY = 100;
	public BuilderSetOfWords(
			Map<String, ? extends LexicalResource<? extends RuleInfo>> resources,
					int depth)
	{
		super();
		this.resources = resources;
		this.depth = depth;
	}
	
	/**
	 * Creates a set of rules. The set is created as map from word-in-text to all
	 * rules whose have this word as left-hand-side. In simple words - creates the
	 * rules that will be returned by {@link #getRulesForRuleBase()}.
	 * <P>
	 * The rules are only rules (i.e., chains) whose right-hand-side is one of the words
	 * given in the argument <tt>hypothesisLemmas</tt>
	 * 
	 * @param hypothesisLemmas the hypothesis words - a set of words for which
	 * the rules are created.
	 * @throws LexicalResourceException
	 * @throws TeEngineMlException
	 */
	public void createRuleBase(ImmutableSet<LemmaAndPos> hypothesisLemmas) throws LexicalResourceException, TeEngineMlException
	{
		Set<CanonicalLemmaAndPos> setCanHypoLap = new LinkedHashSet<CanonicalLemmaAndPos>();
		for (LemmaAndPos hypoLap : hypothesisLemmas)
		{
			setCanHypoLap.add(new CanonicalLemmaAndPos(hypoLap.getLemma(), hypoLap.getPartOfSpeech()));
		}
		allRules = new SimpleValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules>();
		for (CanonicalLemmaAndPos hypothesisLemma : setCanHypoLap)
		{
			ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules> rulesSingleWord = getForSingleHypothesisWord(hypothesisLemma);
			for (CanonicalLemmaAndPos lemmaAndPos : rulesSingleWord.keySet())
			{
				for (ChainOfLexicalRules chainOfLexicalRules : rulesSingleWord.get(lemmaAndPos))
				{
					allRules.put(lemmaAndPos, chainOfLexicalRules);
				}
			}
		}

		rulesForRuleBase = new LinkedHashMap<CanonicalLemmaAndPos, ImmutableSet<ChainOfLexicalRules>>();
		for (CanonicalLemmaAndPos lhs : allRules.keySet())
		{
			rulesForRuleBase.put(lhs,allRules.get(lhs));
		}
	}

	/**
	 * Returns the rules created by {@link #createRuleBase(ImmutableSet)}.
	 * @return the rules created by {@link #createRuleBase(ImmutableSet)}.
	 */
	public Map<CanonicalLemmaAndPos, ImmutableSet<ChainOfLexicalRules>> getRulesForRuleBase()
	{
		return rulesForRuleBase;
	}
	
	/**
	 * Returns a set of the "real" knowledge resources that were used by
	 * this class. For example: WORDNET, BAP, etc.
	 * <P>
	 * This set is not sorted. It is used by {@link DefaultOperationScript}, which
	 * first sorts it, and then use it as subset of the sorted set of the
	 * knowledge-resources used by the system, retrieved by {@link OperationsScript#getRuleBasesNames()}.
	 * See also {@link SafeSamplesUtils#load(java.io.File, java.util.LinkedHashSet)}.
	 * 
	 * @return a set of the "real" knowledge-resources used by this builder.
	 */
	public Set<String> getUnsortedSetOfRuleBasesNames()
	{
		return this.resources.keySet();
	}

	/**
	 * Returns the rules (i.e., chains) whose right-hand-side is the given word.
	 * 
	 * @param hypothesisLemma
	 * @return
	 * @throws LexicalResourceException
	 * @throws TeEngineMlException
	 */
	private ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules> getForSingleHypothesisWord(CanonicalLemmaAndPos hypothesisLemma) throws LexicalResourceException, TeEngineMlException
	{
		boolean retrieved = false;
		ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules> rulesSingleWord = null;
		if (cacheGeneratedForSingleWord.containsKey(hypothesisLemma))
		{
			rulesSingleWord = cacheGeneratedForSingleWord.get(hypothesisLemma);
			retrieved = true;
		}
		if (!retrieved)
		{
			logger.debug("retrieving rules using BuilderSingleWord");
			BuilderSingleWord builderSingleWord = new BuilderSingleWord(hypothesisLemma, resources);
			builderSingleWord.createRuleBase(depth);
			rulesSingleWord = builderSingleWord.getGeneratedRules();
			cacheGeneratedForSingleWord.put(hypothesisLemma, rulesSingleWord);
		}
		return rulesSingleWord;

	}

	// input
	private final Map<String,? extends LexicalResource<? extends RuleInfo>> resources;
	private final int depth;
	
	// output
	private Map<CanonicalLemmaAndPos, ImmutableSet<ChainOfLexicalRules>> rulesForRuleBase = null;
	
	///////
	
	private ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules> allRules;
	
	private Cache<CanonicalLemmaAndPos, ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules>> cacheGeneratedForSingleWord =
		new CacheFactory<CanonicalLemmaAndPos, ValueSetMap<CanonicalLemmaAndPos, ChainOfLexicalRules>>().getCache(CACHE_CAPACITY);
	
	private static final Logger logger = Logger.getLogger(BuilderSetOfWords.class);
}
