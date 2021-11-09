package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class SingleCrawler extends RecursiveAction {
    private final String url;
    private final Duration timeout;
    private  Clock clock;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final List<Pattern> ignoredUrls;
    private final Set<String> visitedUrls;
    private final PageParserFactory factory;
    private final int popularWordCount;

    public SingleCrawler(String url, Duration timeout, int maxDepth,
                         Map<String, Integer> counts,
                         Set<String> visitedUrls, Clock clock,
                         List<Pattern> ignoredUrls, PageParserFactory factory, int popularWordCount){
        this.url = url;
        this.timeout = timeout;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.factory = factory;
        this.popularWordCount = popularWordCount;
        this.clock = clock;
    }

    @Override
    protected void compute(){
        Instant deadline = clock.instant().plus(timeout);
        if(maxDepth == 0 || clock.instant().isAfter(deadline)){
            return;
        }
        for(Pattern pattern : ignoredUrls){
            if(pattern.matcher(url).matches()){
                return;
            }
        }

        if(visitedUrls.contains(url)){
            return;
        }

        visitedUrls.add(url);
        PageParser.Result result = factory.get(url).parse();

        for(ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()){
            counts.compute(entry.getKey(), (k, v) -> (v == null) ? entry.getValue() : entry.getValue() + v);
        }
        List<SingleCrawler> task = new ArrayList<>();

        for(String url : result.getLinks()){
            task.add(new SingleCrawler(url, timeout, maxDepth -1, counts,
                    visitedUrls, clock, ignoredUrls, factory, popularWordCount));
        }

        invokeAll(task);
    }
}
