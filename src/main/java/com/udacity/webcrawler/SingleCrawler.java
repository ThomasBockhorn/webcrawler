package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
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
    private  Clock clock;
    private final int maxDepth;
    private final Map<String, Integer> counts;
    private final List<Pattern> ignoredUrls;
    private final Set<String> visitedUrls;
    private final PageParserFactory factory;
    private final int popularWordCount;
    private final Instant deadline;

    public SingleCrawler(String url, Instant deadline, int maxDepth,
                         Map<String, Integer> counts,
                         Set<String> visitedUrls, Clock clock,
                         List<Pattern> ignoredUrls, PageParserFactory factory, int popularWordCount){
        this.url = url;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
        this.factory = factory;
        this.popularWordCount = popularWordCount;
        this.clock = clock;
        this.deadline = deadline;
    }

    @Override
    protected void compute(){
        if(maxDepth == 0 || clock.instant().isAfter(deadline)){
            return;
        }
        for(Pattern pattern : ignoredUrls){
            if(pattern.matcher(url).matches()){
                return;
            }
        }

        if(!visitedUrls.add(url)){
            return;
        }

        PageParser.Result result = factory.get(url).parse();

        for(ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()){
            counts.compute(entry.getKey(), (k, v) -> (v == null) ? entry.getValue() : entry.getValue() + v);
        }
        List<SingleCrawler> task = new ArrayList<>();

        for(String url : result.getLinks()){
            task.add(new SingleCrawler(url, deadline, maxDepth -1, counts,
                    visitedUrls, clock, ignoredUrls, factory, popularWordCount));
        }

        invokeAll(task);
    }
}
