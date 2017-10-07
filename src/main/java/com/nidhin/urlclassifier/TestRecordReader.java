package com.nidhin.urlclassifier;

import org.datavec.api.conf.Configuration;
import org.datavec.api.records.Record;
import org.datavec.api.records.listener.RecordListener;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.writable.Writable;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Created by nidhin on 25/7/17.
 */
public class TestRecordReader implements RecordReader {
    @Override
    public void initialize(InputSplit inputSplit) throws IOException, InterruptedException {

    }

    @Override
    public void initialize(Configuration configuration, InputSplit inputSplit) throws IOException, InterruptedException {

    }

    @Override
    public List<Writable> next() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public List<Writable> record(URI uri, DataInputStream dataInputStream) throws IOException {
        return null;
    }

    @Override
    public Record nextRecord() {
        return null;
    }

    @Override
    public Record loadFromMetaData(RecordMetaData recordMetaData) throws IOException {
        return null;
    }

    @Override
    public List<Record> loadFromMetaData(List<RecordMetaData> list) throws IOException {
        return null;
    }

    @Override
    public List<RecordListener> getListeners() {
        return null;
    }

    @Override
    public void setListeners(RecordListener... recordListeners) {

    }

    @Override
    public void setListeners(Collection<RecordListener> collection) {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setConf(Configuration configuration) {

    }

    @Override
    public Configuration getConf() {
        return null;
    }
}
